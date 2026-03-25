package com.opticoms.optinmscore.security.encryption;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    // Bu anahtar Environment Variable'dan gelmeli.
    // Eğer gelmezse uygulama güvenli başlamaz.
    @Value("${app.security.master-key}")
    private String masterKeyString;

    private SecretKey secretKey; // İşlenmiş, güvenli anahtar

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 12 bytes IV (Endüstri standardı)
    private static final int GCM_TAG_LENGTH = 128; // 128 bit authentication tag

    @PostConstruct
    public void init() {
        try {
            // 1. Gelen anahtarı ne olursa olsun SHA-256 ile 32 byte (256 bit) sabit uzunluğa getiriyoruz.
            // Bu sayede "Key length" hatalarını önleriz.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(masterKeyString.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize encryption key", e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            // 2. Her işlem için rastgele bir IV (Initialization Vector) oluştur.
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 3. Şifreleme motorunu GCM modunda başlat
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 4. IV ve Şifreli Veriyi birleştir: [IV (12 byte)] + [Şifreli Veri]
            // Çözerken ilk 12 byte'ı ayırıp IV olarak kullanacağız.
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting data", e);
        }
    }

    /**
     * Deterministic SHA-256 hash for searchable encrypted fields.
     * Same input always produces the same hash, enabling DB lookups.
     */
    public String hash(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error while hashing data", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return encryptedText;
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);

            // 5. Veriyi parçala: Önce IV'yi al, kalanı ciphertext.
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv); // İlk 12 byte'ı oku

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText); // Geri kalanı oku

            // 6. Çözme işlemini başlat
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            // Eğer anahtar yanlışsa veya veri bozulmuşsa burası patlar (GCM Integrity Check)
            throw new RuntimeException("Error while decrypting data. Key might be wrong or data corrupted.", e);
        }
    }
}