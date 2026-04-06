package com.opticoms.optinmscore.domain.backup.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCursor;
import com.opticoms.optinmscore.domain.backup.model.BackupEntry;
import com.opticoms.optinmscore.domain.backup.model.BackupEntry.BackupStatus;
import com.opticoms.optinmscore.domain.backup.repository.BackupEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
public class BackupService {

    private static final List<String> BACKUP_COLLECTIONS = List.of(
            "tenants", "users", "subscribers", "apn_profiles", "policies",
            "alarms", "certificates", "firewall_rules", "suci_profiles",
            "edge_locations", "gnodebs", "connected_ues", "pdu_sessions",
            "pm_metrics", "node_resources", "audit_logs", "licenses",
            "global_configs", "amf_configs", "smf_configs", "upf_configs",
            "slave_nodes", "rate_limit_buckets"
    );

    private final MongoTemplate mongoTemplate;
    private final BackupEntryRepository backupEntryRepository;
    private final ObjectMapper objectMapper;
    private final Path storagePath;
    private final int retentionCount;

    public BackupService(MongoTemplate mongoTemplate,
                         BackupEntryRepository backupEntryRepository,
                         ObjectMapper objectMapper,
                         @Value("${app.backup.storage-path:/data/backups}") String storagePath,
                         @Value("${app.backup.retention-count:3}") int retentionCount) {
        this.mongoTemplate = mongoTemplate;
        this.backupEntryRepository = backupEntryRepository;
        this.objectMapper = objectMapper;
        this.storagePath = Paths.get(storagePath);
        this.retentionCount = retentionCount;
    }

    public BackupEntry createBackup(String triggeredBy) {
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")
                .withZone(ZoneOffset.UTC).format(Instant.now());
        String filename = "backup_" + timestamp + ".json.gz";

        BackupEntry entry = new BackupEntry();
        entry.setTenantId(null);
        entry.setFilename(filename);
        entry.setStatus(BackupStatus.IN_PROGRESS);
        entry.setTriggeredBy(triggeredBy);
        entry.setStoragePath(storagePath.resolve(filename).toString());
        entry = backupEntryRepository.save(entry);

        try {
            Files.createDirectories(storagePath);
            Path filePath = storagePath.resolve(filename);

            try (OutputStream os = Files.newOutputStream(filePath);
                 GZIPOutputStream gzip = new GZIPOutputStream(os);
                 JsonGenerator gen = objectMapper.createGenerator(gzip)) {

                gen.writeStartObject();
                for (String collection : BACKUP_COLLECTIONS) {
                    gen.writeArrayFieldStart(collection);
                    try (MongoCursor<Document> cursor = mongoTemplate
                            .getCollection(collection).find().batchSize(500).cursor()) {
                        while (cursor.hasNext()) {
                            gen.writeObject(cursor.next());
                        }
                    }
                    gen.writeEndArray();
                }
                gen.writeEndObject();
            }

            entry.setStatus(BackupStatus.COMPLETED);
            entry.setSizeBytes(Files.size(filePath));
            entry.setCompletedAt(Instant.now().toEpochMilli());
            entry = backupEntryRepository.save(entry);

            enforceRetentionPolicy();
            log.info("Backup completed: {} ({} bytes)", filename, entry.getSizeBytes());

        } catch (Exception e) {
            log.error("Backup failed: {}", e.getMessage(), e);
            entry.setStatus(BackupStatus.FAILED);
            entry.setErrorMessage(e.getMessage());
            entry = backupEntryRepository.save(entry);
        }

        return entry;
    }

    public void restoreBackup(String backupId) {
        BackupEntry entry = backupEntryRepository.findById(backupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Backup not found"));

        if (entry.getStatus() != BackupStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only completed backups can be restored");
        }

        Path filePath = Paths.get(entry.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Backup file not found on disk");
        }

        List<Document> superAdmins = mongoTemplate.find(
                Query.query(Criteria.where("role").is("SUPER_ADMIN")),
                Document.class, "users");

        try (InputStream is = Files.newInputStream(filePath);
             GZIPInputStream gzip = new GZIPInputStream(is)) {

            Map<String, List<Map<String, Object>>> data = objectMapper.readValue(
                    gzip, new TypeReference<>() {});

            for (Map.Entry<String, List<Map<String, Object>>> collEntry : data.entrySet()) {
                String collectionName = collEntry.getKey();
                if ("backup_entries".equals(collectionName)) continue;

                List<Map<String, Object>> documents = collEntry.getValue();
                mongoTemplate.dropCollection(collectionName);
                if (!documents.isEmpty()) {
                    List<Document> bsonDocs = documents.stream()
                            .map(Document::new)
                            .toList();
                    mongoTemplate.insert(bsonDocs, collectionName);
                }
            }

            if (!superAdmins.isEmpty()) {
                long superAdminCount = mongoTemplate.count(
                        Query.query(Criteria.where("role").is("SUPER_ADMIN")), "users");
                if (superAdminCount == 0) {
                    mongoTemplate.insert(superAdmins, "users");
                    log.warn("SUPER_ADMIN accounts restored after backup restore");
                }
            }

            log.info("Restore completed from backup: {}", entry.getFilename());

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Restore failed — system may be in inconsistent state: "
                    + e.getMessage(), e);
        }
    }

    public List<BackupEntry> listBackups() {
        return backupEntryRepository.findAllByOrderByCreatedAtDesc();
    }

    public byte[] getBackupFile(String backupId) {
        BackupEntry entry = backupEntryRepository.findById(backupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Backup not found"));

        Path filePath = Paths.get(entry.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Backup file not found on disk");
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read backup file");
        }
    }

    public void deleteBackup(String backupId) {
        BackupEntry entry = backupEntryRepository.findById(backupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Backup not found"));

        try {
            Path filePath = Paths.get(entry.getStoragePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete backup file: {}", e.getMessage());
        }

        backupEntryRepository.delete(entry);
    }

    void enforceRetentionPolicy() {
        List<BackupEntry> completed = backupEntryRepository
                .findByStatusOrderByCreatedAtDesc(BackupStatus.COMPLETED);

        if (completed.size() <= retentionCount) return;

        List<BackupEntry> toDelete = completed.subList(retentionCount, completed.size());
        for (BackupEntry old : toDelete) {
            try {
                Path filePath = Paths.get(old.getStoragePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Failed to delete old backup file {}: {}", old.getFilename(), e.getMessage());
            }
            backupEntryRepository.delete(old);
            log.info("Retention policy: deleted old backup {}", old.getFilename());
        }
    }
}
