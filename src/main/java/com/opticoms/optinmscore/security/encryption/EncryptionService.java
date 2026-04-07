package com.opticoms.optinmscore.security.encryption;

import com.opticoms.optinmscore.common.exception.EncryptionException;
import com.opticoms.optinmscore.config.encryption.EncryptionMetadata;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionService {

    @Value("${app.security.master-key}")
    private String masterKeyString;

    private final MongoTemplate mongoTemplate;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 100_000;

    public EncryptionService(@Lazy MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void init() {
        if (masterKeyString == null || masterKeyString.isBlank()) {
            throw new EncryptionException(
                    "app.security.master-key is not configured. " +
                    "Application cannot start without a valid encryption key.");
        }
        try {
            byte[] salt = resolveOrCreateSalt();
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                    masterKeyString.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new EncryptionException("Could not initialize encryption key", e);
        }
    }

    private byte[] resolveOrCreateSalt() {
        EncryptionMetadata meta = mongoTemplate.findById("MASTER", EncryptionMetadata.class);
        if (meta != null) {
            return Base64.getDecoder().decode(meta.getSaltBase64());
        }
        byte[] newSalt = new byte[16];
        new SecureRandom().nextBytes(newSalt);
        meta = new EncryptionMetadata();
        meta.setId("MASTER");
        meta.setSaltBase64(Base64.getEncoder().encodeToString(newSalt));
        mongoTemplate.save(meta);
        log.info("New encryption salt generated and persisted to MongoDB");
        return newSalt;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Concatenate IV (12 bytes) + ciphertext for storage; decrypt splits the prefix as IV.
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new EncryptionException("Error while encrypting data", e);
        }
    }

    /**
     * Keyed HMAC-SHA256 hash for searchable encrypted fields.
     * Same input + same key always produces the same hash, enabling DB lookups.
     * Without the key, rainbow table attacks are infeasible.
     */
    public String hash(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new EncryptionException("Error while hashing data", e);
        }
    }

    public String decrypt(String encryptedText) {
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
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new EncryptionException("Error while decrypting data. Key might be wrong or data corrupted.", e);
        }
    }
}