package com.opticoms.optinmscore.domain.certificate.service;

import com.opticoms.optinmscore.domain.certificate.model.CertificateEntry;
import com.opticoms.optinmscore.domain.certificate.repository.CertificateRepository;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String ENCRYPTED = "ENC_DATA";

    // Valid self-signed cert: CN=test.open5gs.org, valid 2026-2036
    private static final String VALID_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDGDCCAgCgAwIBAgIJAKKsPYaGQNWWMA0GCSqGSIb3DQEBCwUAMDoxCzAJBgNV
            BAYTAlRSMRAwDgYDVQQKEwdPcHRpTk1TMRkwFwYDVQQDExB0ZXN0Lm9wZW41Z3Mu
            b3JnMB4XDTI2MDMxNzE1MDgzMloXDTM2MDMxNDE1MDgzMlowOjELMAkGA1UEBhMC
            VFIxEDAOBgNVBAoTB09wdGlOTVMxGTAXBgNVBAMTEHRlc3Qub3BlbjVncy5vcmcw
            ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDHzSJVjqBNcjx9XD0XJNbk
            WJ7hsY4JtOaWQRG8vhYDCJaXgRldj3dkE/kPMt4LiPsbSFrq1ap688dRe8vClIRK
            cyNd3VLR3Kz1Ox6e1pD6zcYYXDXxMaU3mdZr5U6sSs86f/Hq9F9Xxut1+GLuwcUv
            HioaYd6AJ6gcDXsMaB9rnc44o+zaxdYGw37N+H+XXNgzWnxkAvKLWAyVXZu7NiU5
            GDPNYujmd5nTrJqCq0ytbJMT1Z2ci5jd1K+uRVHVSkMW9jcEAZNUJWKv1GCWUP/t
            mf1BLh1ITmhIIOQig/JSOof79IuWlPeaH7iKn1AC9CS/6aFYYDVLGs9t3N/1NzzT
            AgMBAAGjITAfMB0GA1UdDgQWBBSUp618JwTx16wXIJXxbiWto4idnzANBgkqhkiG
            9w0BAQsFAAOCAQEAL0RVV/YR2IBj0Td4QLMWRRQ/ujwO/T8vM1Ezgx4Mo0lxwFxY
            Y+8EItjdZluSU0e/n4v+BksnNB6lIVrXtzTI8RCrMQghzxcBXjRYTerAAx6c51FM
            TWd+AhsbuDAoPtzenEWrY5pixB9aaeXZFr+LHxvysjwRuKy6j5SZRzq3OkAkaDN/
            KG55/do5obnNSZ85Y+9QS/ol7HxuLv1rCn2vPbsQIBDN0VvKwPN633NuCDKrS+Br
            Sx4pdfr3xRUB5fsjZfhEAucgizkGuf9ifhtR64OlTtNt4mms1tcg90aSWZh+VQLo
            8XbULlQdfPiWo/dBSpz9qJ/LhVNFVUmxrB+CPQ==
            -----END CERTIFICATE-----""";

    // Expired self-signed cert: CN=expired.open5gs.org, expired 2020-01-01
    private static final String EXPIRED_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDHTCCAgWgAwIBAgIIBzFGh2tz0P8wDQYJKoZIhvcNAQELBQAwPTELMAkGA1UE
            BhMCVFIxEDAOBgNVBAoTB09wdGlOTVMxHDAaBgNVBAMTE2V4cGlyZWQub3BlbjVn
            cy5vcmcwHhcNMTkxMjMxMjEwMDAwWhcNMjAwMTAxMjEwMDAwWjA9MQswCQYDVQQG
            EwJUUjEQMA4GA1UEChMHT3B0aU5NUzEcMBoGA1UEAxMTZXhwaXJlZC5vcGVuNWdz
            Lm9yZzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJdqxmj2ExTeq3li
            0CVwABn1zH5Lvi/lmsn7ka8wr6EplKismg44t+mVP+pYSD3ftjMroZ8+EOzUgKU1
            jb2YLEr1+39tXxB0kDf5+41lD/M4Z+rsFj2PaR2mbq9DntlHB8WpOlHFs84gt7Uu
            L0L3+Gk/zs7hTUIWnmoiBKpr40QvkL4wBx+n3Tp/pdv1YKiNPz8MxjW62M3ZCz40
            xdIgW+bodcziRt3sDXp5FXB7TVb6rx4QAgswo/qbHovA0EJz7MRu+kNceJagGroP
            YZS6EFwxHlIVwvW6OG3Hv2IZb+dAgM+mzmx/8aGlAFKVtddrhrRCq3Sv2Kn6xLJ+
            pN/ZUKMCAwEAAaMhMB8wHQYDVR0OBBYEFHSZ3FiM03xkpQpiNOj0P9QvBceNMA0G
            CSqGSIb3DQEBCwUAA4IBAQB/zs0ie1KApV4kcGu4IBw9fi/gtUvjWcR4cIWwR5EL
            4K7Aj9K5lNbhXWKkzZgtenqMVN+vBO4aFHbQ5Lo5IMQ7clcHMjwrRlnzf0l1U/Lp
            +ZhZ319jdICVpbVfJsvcRKrA9pjOPqWYn3Jre+yXJ+qMJK6Cral33yVJnBbpXCZw
            RMbtN2Mk0kxfY0W8xJ2XpHEs6tg8OkenPTFhHWZy4//rF3ijM1xkBisojs+Yv/lS
            M6t3zy6D5CHEyZNFk0u1doE1Lf3Px7PKZspU8ujK4dISLk6RkLp/jDK7YY0IGOji
            OQqPrMdoZ3mAHQiaHigbgViycQyBzuDQ/mly8tU0A3eZ
            -----END CERTIFICATE-----""";

    @Mock private CertificateRepository repository;
    @Mock private EncryptionService encryptionService;
    @InjectMocks private CertificateService service;

    private CertificateEntry sampleEntry;

    @BeforeEach
    void setUp() {
        sampleEntry = new CertificateEntry();
        sampleEntry.setId("cert-1");
        sampleEntry.setTenantId(TENANT);
        sampleEntry.setName("amf-server-cert");
        sampleEntry.setCertType(CertificateEntry.CertType.SERVER);
        sampleEntry.setCertificatePem(VALID_PEM);
        sampleEntry.setPrivateKeyPem("-----BEGIN PRIVATE KEY-----\nfake\n-----END PRIVATE KEY-----");
        sampleEntry.setStatus(CertificateEntry.CertStatus.ACTIVE);
        sampleEntry.setSubjectDn("CN=test.open5gs.org");
        sampleEntry.setDescription("Test cert");
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Nested
    class Create {

        @Test
        void create_validPem_extractsX509FieldsAndEncryptsKey() {
            when(repository.findByTenantIdAndName(TENANT, "amf-server-cert"))
                    .thenReturn(Optional.empty());
            when(encryptionService.encrypt(anyString())).thenReturn(ENCRYPTED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CertificateEntry input = new CertificateEntry();
            input.setName("amf-server-cert");
            input.setCertType(CertificateEntry.CertType.SERVER);
            input.setCertificatePem(VALID_PEM);
            input.setPrivateKeyPem("-----BEGIN PRIVATE KEY-----\nfake\n-----END PRIVATE KEY-----");

            CertificateEntry result = service.create(TENANT, input);

            assertThat(result.getTenantId()).isEqualTo(TENANT);
            assertThat(result.getSubjectDn()).contains("test.open5gs.org");
            assertThat(result.getIssuerDn()).isNotBlank();
            assertThat(result.getSerialNumber()).isNotBlank();
            assertThat(result.getNotBefore()).isNotNull();
            assertThat(result.getNotAfter()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(CertificateEntry.CertStatus.ACTIVE);
            assertThat(result.getPrivateKeyPem()).isEqualTo(ENCRYPTED);
        }

        @Test
        void create_expiredCert_setsExpiredStatus() {
            when(repository.findByTenantIdAndName(TENANT, "old-cert"))
                    .thenReturn(Optional.empty());
            when(encryptionService.encrypt(anyString())).thenReturn(ENCRYPTED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CertificateEntry input = new CertificateEntry();
            input.setName("old-cert");
            input.setCertType(CertificateEntry.CertType.CA);
            input.setCertificatePem(EXPIRED_PEM);
            input.setPrivateKeyPem("key");

            CertificateEntry result = service.create(TENANT, input);

            assertThat(result.getStatus()).isEqualTo(CertificateEntry.CertStatus.EXPIRED);
        }

        @Test
        void create_duplicateName_throwsConflict() {
            CertificateEntry existing = new CertificateEntry();
            existing.setId("other-id");
            when(repository.findByTenantIdAndName(TENANT, "amf-server-cert"))
                    .thenReturn(Optional.of(existing));

            CertificateEntry input = new CertificateEntry();
            input.setName("amf-server-cert");
            input.setCertType(CertificateEntry.CertType.SERVER);
            input.setCertificatePem(VALID_PEM);

            assertThatThrownBy(() -> service.create(TENANT, input))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        void create_invalidPem_throwsBadRequest() {
            CertificateEntry input = new CertificateEntry();
            input.setName("bad-cert");
            input.setCertType(CertificateEntry.CertType.SERVER);
            input.setCertificatePem("NOT A VALID PEM");

            assertThatThrownBy(() -> service.create(TENANT, input))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid PEM");
        }

        @Test
        void create_nullPrivateKey_noEncryptionAttempt() {
            when(repository.findByTenantIdAndName(TENANT, "ca-cert"))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CertificateEntry input = new CertificateEntry();
            input.setName("ca-cert");
            input.setCertType(CertificateEntry.CertType.CA);
            input.setCertificatePem(VALID_PEM);

            service.create(TENANT, input);

            verify(encryptionService, never()).encrypt(anyString());
        }
    }

    // ── Read ────────────────────────────────────────────────────────────

    @Nested
    class Read {

        @Test
        void getById_found_masksPrivateKey() {
            sampleEntry.setPrivateKeyPem(ENCRYPTED);
            when(repository.findByIdAndTenantId("cert-1", TENANT))
                    .thenReturn(Optional.of(sampleEntry));

            CertificateEntry result = service.getById(TENANT, "cert-1");

            assertThat(result.getId()).isEqualTo("cert-1");
            assertThat(result.getPrivateKeyPem()).isNull();
        }

        @Test
        void getById_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(TENANT, "missing"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        void list_delegatesAndMasksKeys() {
            sampleEntry.setPrivateKeyPem(ENCRYPTED);
            Pageable pageable = PageRequest.of(0, 20);
            when(repository.findByTenantId(TENANT, pageable))
                    .thenReturn(new PageImpl<>(List.of(sampleEntry)));

            Page<CertificateEntry> result = service.list(TENANT, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getPrivateKeyPem()).isNull();
        }

        @Test
        void listByType_delegatesCorrectly() {
            when(repository.findByTenantIdAndCertType(TENANT, CertificateEntry.CertType.SERVER))
                    .thenReturn(List.of(sampleEntry));

            List<CertificateEntry> result = service.listByType(TENANT, CertificateEntry.CertType.SERVER);

            assertThat(result).hasSize(1);
        }

        @Test
        void listByStatus_delegatesCorrectly() {
            when(repository.findByTenantIdAndStatus(TENANT, CertificateEntry.CertStatus.ACTIVE))
                    .thenReturn(List.of(sampleEntry));

            List<CertificateEntry> result = service.listByStatus(TENANT, CertificateEntry.CertStatus.ACTIVE);

            assertThat(result).hasSize(1);
        }

        @Test
        void listExpiringSoon_usesCorrectCutoff() {
            when(repository.findByTenantIdAndNotAfterLessThanAndStatusNot(
                    eq(TENANT), anyLong(), eq(CertificateEntry.CertStatus.REVOKED)))
                    .thenReturn(List.of(sampleEntry));

            List<CertificateEntry> result = service.listExpiringSoon(TENANT, 30);

            assertThat(result).hasSize(1);
            verify(repository).findByTenantIdAndNotAfterLessThanAndStatusNot(
                    eq(TENANT), anyLong(), eq(CertificateEntry.CertStatus.REVOKED));
        }
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Nested
    class Update {

        @Test
        void update_activeEntry_updatesFieldsAndEncrypts() {
            when(repository.findByIdAndTenantId("cert-1", TENANT))
                    .thenReturn(Optional.of(sampleEntry));
            when(repository.findByTenantIdAndName(TENANT, "amf-server-cert"))
                    .thenReturn(Optional.of(sampleEntry));
            when(encryptionService.encrypt(anyString())).thenReturn(ENCRYPTED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CertificateEntry updated = new CertificateEntry();
            updated.setName("amf-server-cert");
            updated.setCertType(CertificateEntry.CertType.SERVER);
            updated.setCertificatePem(VALID_PEM);
            updated.setPrivateKeyPem("new-key");
            updated.setDescription("Updated desc");

            CertificateEntry result = service.update(TENANT, "cert-1", updated);

            assertThat(result.getDescription()).isEqualTo("Updated desc");
            assertThat(result.getPrivateKeyPem()).isEqualTo(ENCRYPTED);
        }

        @Test
        void update_revokedEntry_throwsConflict() {
            sampleEntry.setStatus(CertificateEntry.CertStatus.REVOKED);
            when(repository.findByIdAndTenantId("cert-1", TENANT))
                    .thenReturn(Optional.of(sampleEntry));

            assertThatThrownBy(() -> service.update(TENANT, "cert-1", new CertificateEntry()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("revoked");
        }

        @Test
        void update_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TENANT, "missing", new CertificateEntry()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Nested
    class Delete {

        @Test
        void delete_found_deletesEntry() {
            when(repository.findByIdAndTenantId("cert-1", TENANT))
                    .thenReturn(Optional.of(sampleEntry));

            service.delete(TENANT, "cert-1");

            verify(repository).delete(sampleEntry);
        }

        @Test
        void delete_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(TENANT, "missing"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── Revoke ──────────────────────────────────────────────────────────

    @Nested
    class Revoke {

        @Test
        void revoke_activeEntry_setsRevoked() {
            when(repository.findByIdAndTenantId("cert-1", TENANT))
                    .thenReturn(Optional.of(sampleEntry));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CertificateEntry result = service.revoke(TENANT, "cert-1");

            assertThat(result.getStatus()).isEqualTo(CertificateEntry.CertStatus.REVOKED);
        }

        @Test
        void revoke_alreadyRevoked_throwsConflict() {
            sampleEntry.setStatus(CertificateEntry.CertStatus.REVOKED);
            when(repository.findByIdAndTenantId("cert-1", TENANT))
                    .thenReturn(Optional.of(sampleEntry));

            assertThatThrownBy(() -> service.revoke(TENANT, "cert-1"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already revoked");
        }

        @Test
        void revoke_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revoke(TENANT, "missing"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── Count ───────────────────────────────────────────────────────────

    @Nested
    class Count {

        @Test
        void count_delegatesToRepository() {
            when(repository.countByTenantId(TENANT)).thenReturn(8L);
            assertThat(service.count(TENANT)).isEqualTo(8);
        }

        @Test
        void countByStatus_delegatesToRepository() {
            when(repository.countByTenantIdAndStatus(TENANT, CertificateEntry.CertStatus.ACTIVE))
                    .thenReturn(5L);
            assertThat(service.countByStatus(TENANT, CertificateEntry.CertStatus.ACTIVE)).isEqualTo(5);
        }
    }

    // ── PEM parsing ─────────────────────────────────────────────────────

    @Nested
    class PemParsing {

        @Test
        void parsePem_validCert_returnsX509() {
            X509Certificate x509 = service.parsePem(VALID_PEM);
            assertThat(x509).isNotNull();
            assertThat(x509.getSubjectX500Principal().getName()).contains("test.open5gs.org");
        }

        @Test
        void parsePem_expiredCert_stillParsesSuccessfully() {
            X509Certificate x509 = service.parsePem(EXPIRED_PEM);
            assertThat(x509).isNotNull();
            assertThat(x509.getSubjectX500Principal().getName()).contains("expired.open5gs.org");
            assertThat(x509.getNotAfter().getTime()).isLessThan(System.currentTimeMillis());
        }

        @Test
        void parsePem_invalidData_throwsBadRequest() {
            assertThatThrownBy(() -> service.parsePem("garbage data"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid PEM");
        }
    }
}
