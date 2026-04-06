package com.opticoms.optinmscore.config.migration;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

/**
 * One-time migration: re-encrypts subscriber data from old SHA-256 derived key
 * to new PBKDF2 derived key, and re-hashes IMSI/MSISDN with HMAC-SHA256.
 *
 * Activate by setting: app.migration.re-encrypt-subscribers=true
 * After successful run, remove the flag to prevent re-runs.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class EncryptionMigrationRunner implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;
    private final EncryptionService encryptionService;

    @Value("${app.security.master-key}")
    private String masterKeyString;

    @Value("${app.migration.re-encrypt-subscribers:false}")
    private boolean migrationEnabled;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Override
    public void run(String... args) {
        if (!migrationEnabled) {
            return;
        }

        log.warn("=== ENCRYPTION MIGRATION STARTED ===");

        SecretKey oldKey = deriveOldKey(masterKeyString);
        List<Subscriber> allSubscribers = mongoTemplate.find(new Query(), Subscriber.class);
        log.info("Found {} subscribers to migrate", allSubscribers.size());

        int success = 0;
        int failed = 0;

        for (Subscriber sub : allSubscribers) {
            try {
                String plainImsi = decryptWithOldKey(sub.getImsi(), oldKey);
                String plainKi = decryptWithOldKey(sub.getKi(), oldKey);
                String plainOpc = sub.getOpc() != null ? decryptWithOldKey(sub.getOpc(), oldKey) : null;
                String plainOp = sub.getOp() != null ? decryptWithOldKey(sub.getOp(), oldKey) : null;
                String plainMsisdn = sub.getMsisdn() != null ? decryptWithOldKey(sub.getMsisdn(), oldKey) : null;

                sub.setImsi(encryptionService.encrypt(plainImsi));
                sub.setImsiHash(encryptionService.hash(plainImsi));
                sub.setKi(encryptionService.encrypt(plainKi));

                if (plainOpc != null) sub.setOpc(encryptionService.encrypt(plainOpc));
                if (plainOp != null) sub.setOp(encryptionService.encrypt(plainOp));
                if (plainMsisdn != null) {
                    sub.setMsisdn(encryptionService.encrypt(plainMsisdn));
                    sub.setMsisdnHash(encryptionService.hash(plainMsisdn));
                }

                mongoTemplate.save(sub);
                success++;
            } catch (Exception e) {
                log.error("Failed to migrate subscriber id={}: {}", sub.getId(), e.getMessage());
                failed++;
            }
        }

        log.warn("=== ENCRYPTION MIGRATION COMPLETED: {} success, {} failed ===", success, failed);
        if (failed > 0) {
            log.error("Some subscribers failed migration. Do NOT disable the old key until all are migrated.");
        }
    }

    private SecretKey deriveOldKey(String keyString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keyString.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Could not derive old encryption key", e);
        }
    }

    private String decryptWithOldKey(String encryptedText, SecretKey oldKey) {
        if (encryptedText == null || encryptedText.isEmpty()) return encryptedText;
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, oldKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt with old key", e);
        }
    }
}
