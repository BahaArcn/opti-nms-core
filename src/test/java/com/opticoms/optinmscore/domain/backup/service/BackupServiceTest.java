package com.opticoms.optinmscore.domain.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.backup.model.BackupEntry;
import com.opticoms.optinmscore.domain.backup.model.BackupEntry.BackupStatus;
import com.opticoms.optinmscore.domain.backup.repository.BackupEntryRepository;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BackupServiceTest {

    @Mock private MongoTemplate mongoTemplate;
    @Mock private BackupEntryRepository backupEntryRepository;

    @TempDir Path tempDir;

    private BackupService backupService;

    @BeforeEach
    void setUp() {
        backupService = new BackupService(
                mongoTemplate,
                backupEntryRepository,
                new ObjectMapper(),
                tempDir.toString(),
                3
        );
    }

    @SuppressWarnings("unchecked")
    private void stubMongoStreaming(List<Document> docs) {
        when(mongoTemplate.getCollection(anyString())).thenAnswer(inv -> {
            MongoCollection<Document> mockCollection = mock(MongoCollection.class);
            FindIterable<Document> mockIterable = mock(FindIterable.class);
            when(mockIterable.batchSize(anyInt())).thenReturn(mockIterable);
            Iterator<Document> it = new ArrayList<>(docs).iterator();
            MongoCursor<Document> mockCursor = mock(MongoCursor.class);
            when(mockCursor.hasNext()).thenAnswer(i -> it.hasNext());
            when(mockCursor.next()).thenAnswer(i -> it.next());
            when(mockIterable.cursor()).thenReturn(mockCursor);
            when(mockCollection.find()).thenReturn(mockIterable);
            return mockCollection;
        });
    }

    @Test
    void createBackup_savesEntryAndFile() {
        when(backupEntryRepository.save(any(BackupEntry.class)))
                .thenAnswer(inv -> {
                    BackupEntry e = inv.getArgument(0);
                    if (e.getId() == null) e.setId("backup-1");
                    return e;
                });
        stubMongoStreaming(List.of(new Document("key", "value")));
        when(backupEntryRepository.findByStatusOrderByCreatedAtDesc(BackupStatus.COMPLETED))
                .thenReturn(new ArrayList<>());

        BackupEntry result = backupService.createBackup("admin");

        assertEquals(BackupStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getFilename());
        assertTrue(result.getFilename().startsWith("backup_"));
        assertTrue(result.getSizeBytes() > 0);
        assertEquals("admin", result.getTriggeredBy());

        verify(backupEntryRepository, atLeast(2)).save(any(BackupEntry.class));

        assertTrue(Files.exists(tempDir.resolve(result.getFilename())),
                "Backup file should exist on disk");
    }

    @Test
    void createBackup_enforcesRetention_deletesOldestWhenOver3() {
        when(backupEntryRepository.save(any(BackupEntry.class)))
                .thenAnswer(inv -> {
                    BackupEntry e = inv.getArgument(0);
                    if (e.getId() == null) e.setId("new-backup");
                    return e;
                });
        stubMongoStreaming(List.of());

        BackupEntry old1 = buildEntry("old-1", "backup_old1.json.gz", tempDir);
        BackupEntry old2 = buildEntry("old-2", "backup_old2.json.gz", tempDir);
        BackupEntry keep1 = buildEntry("keep-1", "backup_k1.json.gz", tempDir);
        BackupEntry keep2 = buildEntry("keep-2", "backup_k2.json.gz", tempDir);
        BackupEntry keep3 = buildEntry("keep-3", "backup_k3.json.gz", tempDir);

        when(backupEntryRepository.findByStatusOrderByCreatedAtDesc(BackupStatus.COMPLETED))
                .thenReturn(new ArrayList<>(List.of(keep3, keep2, keep1, old1, old2)));

        backupService.createBackup("admin");

        verify(backupEntryRepository).delete(old1);
        verify(backupEntryRepository).delete(old2);
        verify(backupEntryRepository, never()).delete(keep1);
        verify(backupEntryRepository, never()).delete(keep2);
        verify(backupEntryRepository, never()).delete(keep3);
    }

    @Test
    void restoreBackup_notFound_throws404() {
        when(backupEntryRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> backupService.restoreBackup("missing"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void restoreBackup_statusNotCompleted_throws400() {
        BackupEntry entry = new BackupEntry();
        entry.setId("in-prog");
        entry.setStatus(BackupStatus.IN_PROGRESS);

        when(backupEntryRepository.findById("in-prog")).thenReturn(Optional.of(entry));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> backupService.restoreBackup("in-prog"));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void listBackups_returnsAllEntries() {
        BackupEntry e1 = new BackupEntry();
        e1.setId("1");
        BackupEntry e2 = new BackupEntry();
        e2.setId("2");

        when(backupEntryRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(e1, e2));

        List<BackupEntry> result = backupService.listBackups();

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getId());
        verify(backupEntryRepository).findAllByOrderByCreatedAtDesc();
    }

    private BackupEntry buildEntry(String id, String filename, Path dir) {
        try {
            Files.createFile(dir.resolve(filename));
        } catch (Exception ignored) {}

        BackupEntry e = new BackupEntry();
        e.setId(id);
        e.setFilename(filename);
        e.setStatus(BackupStatus.COMPLETED);
        e.setStoragePath(dir.resolve(filename).toString());
        return e;
    }
}
