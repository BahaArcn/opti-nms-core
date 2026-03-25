package com.opticoms.optinmscore.security.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new EncryptionService();
        var field = EncryptionService.class.getDeclaredField("masterKeyString");
        field.setAccessible(true);
        field.set(service, "TestMasterKeyForUnitTests123456");
        service.init();
    }

    @Test
    void encrypt_decrypt_roundTrip() {
        String original = "999700000000001";
        String encrypted = service.encrypt(original);

        assertNotEquals(original, encrypted, "Encrypted should differ from original");
        assertFalse(encrypted.contains(original), "Encrypted should not contain plaintext");

        String decrypted = service.decrypt(encrypted);
        assertEquals(original, decrypted, "Decrypted should match original");
    }

    @Test
    void encrypt_sameInput_differentOutput() {
        String input = "TestData12345";
        String enc1 = service.encrypt(input);
        String enc2 = service.encrypt(input);

        assertNotEquals(enc1, enc2, "Each encryption should produce unique ciphertext (random IV)");

        assertEquals(input, service.decrypt(enc1));
        assertEquals(input, service.decrypt(enc2));
    }

    @Test
    void encrypt_null_returnsNull() {
        assertNull(service.encrypt(null));
    }

    @Test
    void encrypt_empty_returnsEmpty() {
        assertEquals("", service.encrypt(""));
    }

    @Test
    void decrypt_null_returnsNull() {
        assertNull(service.decrypt(null));
    }

    @Test
    void decrypt_empty_returnsEmpty() {
        assertEquals("", service.decrypt(""));
    }

    @Test
    void hash_deterministic() {
        String input = "999700000000001";
        String hash1 = service.hash(input);
        String hash2 = service.hash(input);

        assertEquals(hash1, hash2, "Hash should be deterministic");
        assertEquals(64, hash1.length(), "SHA-256 hex should be 64 chars");
    }

    @Test
    void hash_differentInputs_differentHashes() {
        String h1 = service.hash("IMSI_001");
        String h2 = service.hash("IMSI_002");
        assertNotEquals(h1, h2);
    }

    @Test
    void hash_null_returnsNull() {
        assertNull(service.hash(null));
    }

    @Test
    void hash_empty_returnsEmpty() {
        assertEquals("", service.hash(""));
    }

    @Test
    void encrypt_longText_worksCorrectly() {
        String longText = "A".repeat(10000);
        String encrypted = service.encrypt(longText);
        String decrypted = service.decrypt(encrypted);
        assertEquals(longText, decrypted);
    }

    @Test
    void encrypt_specialCharacters() {
        String special = "Ki=0123456789abcdef0123456789abcdef ÖÜÇ ığüşç";
        String encrypted = service.encrypt(special);
        assertEquals(special, service.decrypt(encrypted));
    }

    @Test
    void decrypt_wrongKey_throwsException() throws Exception {
        String encrypted = service.encrypt("secret-data");

        EncryptionService otherService = new EncryptionService();
        var field = EncryptionService.class.getDeclaredField("masterKeyString");
        field.setAccessible(true);
        field.set(otherService, "DifferentMasterKeyForTesting999");
        otherService.init();

        assertThrows(RuntimeException.class, () -> otherService.decrypt(encrypted));
    }

    @Test
    void decrypt_corruptedData_throwsException() {
        assertThrows(Exception.class, () -> service.decrypt("not-valid-base64!!!"));
    }
}
