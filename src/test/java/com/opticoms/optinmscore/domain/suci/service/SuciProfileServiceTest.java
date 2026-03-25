package com.opticoms.optinmscore.domain.suci.service;

import com.opticoms.optinmscore.domain.suci.model.SuciProfile;
import com.opticoms.optinmscore.domain.suci.repository.SuciProfileRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuciProfileServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String PROFILE_A_PUB_KEY =
            "5a8d38864820197c3394b92613b20b91633cbd897119273bf8e4a6f4eec0a650";
    private static final String PROFILE_A_PRIV_KEY =
            "c53c2208b4d1b100f0599b5b9856856fe665df1d2eab0978000b83e8fb721b4a";
    private static final String PROFILE_B_PUB_KEY =
            "0272a3e38c48067dee81dca38bcdb1dc2c2b0087f3e48a5c4e2eb4e55f26411780";
    private static final String PROFILE_B_PRIV_KEY =
            "f1ab1074477bbbcadf3d9da1df9549ec3ff9cab2836c135e30aafbbaf7bfc42d";
    private static final String ENCRYPTED = "ENC_DATA";

    @Mock private SuciProfileRepository repository;
    @Mock private EncryptionService encryptionService;
    @InjectMocks private SuciProfileService service;

    private SuciProfile sampleProfile;

    @BeforeEach
    void setUp() {
        sampleProfile = new SuciProfile();
        sampleProfile.setId("suci-1");
        sampleProfile.setTenantId(TENANT);
        sampleProfile.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_A);
        sampleProfile.setHomeNetworkPublicKeyId(1);
        sampleProfile.setHomeNetworkPublicKey(PROFILE_A_PUB_KEY);
        sampleProfile.setHomeNetworkPrivateKey(PROFILE_A_PRIV_KEY);
        sampleProfile.setKeyStatus(SuciProfile.KeyStatus.ACTIVE);
        sampleProfile.setDescription("Test Profile A key");
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Nested
    class Create {

        @Test
        void create_profileA_setsTenantAndEncryptsPrivateKey() {
            when(repository.findByTenantIdAndHomeNetworkPublicKeyIdAndProtectionScheme(
                    TENANT, 1, SuciProfile.ProtectionScheme.PROFILE_A))
                    .thenReturn(Optional.empty());
            when(encryptionService.encrypt(PROFILE_A_PRIV_KEY)).thenReturn(ENCRYPTED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SuciProfile input = new SuciProfile();
            input.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_A);
            input.setHomeNetworkPublicKeyId(1);
            input.setHomeNetworkPublicKey(PROFILE_A_PUB_KEY);
            input.setHomeNetworkPrivateKey(PROFILE_A_PRIV_KEY);
            input.setDescription("New key");

            SuciProfile result = service.create(TENANT, input);

            assertThat(result.getTenantId()).isEqualTo(TENANT);
            assertThat(result.getKeyStatus()).isEqualTo(SuciProfile.KeyStatus.ACTIVE);
            assertThat(result.getHomeNetworkPrivateKey()).isEqualTo(ENCRYPTED);

            ArgumentCaptor<SuciProfile> captor = ArgumentCaptor.forClass(SuciProfile.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT);
        }

        @Test
        void create_duplicateKeyId_throwsConflict() {
            SuciProfile existing = new SuciProfile();
            existing.setId("other-id");
            when(repository.findByTenantIdAndHomeNetworkPublicKeyIdAndProtectionScheme(
                    TENANT, 1, SuciProfile.ProtectionScheme.PROFILE_A))
                    .thenReturn(Optional.of(existing));

            SuciProfile input = new SuciProfile();
            input.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_A);
            input.setHomeNetworkPublicKeyId(1);
            input.setHomeNetworkPublicKey(PROFILE_A_PUB_KEY);
            input.setHomeNetworkPrivateKey(PROFILE_A_PRIV_KEY);

            assertThatThrownBy(() -> service.create(TENANT, input))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        void create_invalidHexPublicKey_throwsBadRequest() {
            SuciProfile input = new SuciProfile();
            input.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_A);
            input.setHomeNetworkPublicKeyId(1);
            input.setHomeNetworkPublicKey("NOT_VALID_HEX!");
            input.setHomeNetworkPrivateKey(PROFILE_A_PRIV_KEY);

            assertThatThrownBy(() -> service.create(TENANT, input))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("valid hex");
        }

        @Test
        void create_invalidHexPrivateKey_throwsBadRequest() {
            SuciProfile input = new SuciProfile();
            input.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_A);
            input.setHomeNetworkPublicKeyId(1);
            input.setHomeNetworkPublicKey(PROFILE_A_PUB_KEY);
            input.setHomeNetworkPrivateKey("ZZZZ_INVALID");

            assertThatThrownBy(() -> service.create(TENANT, input))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("valid hex");
        }

        @Test
        void create_profileA_wrongPublicKeyLength_throwsBadRequest() {
            SuciProfile input = new SuciProfile();
            input.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_A);
            input.setHomeNetworkPublicKeyId(1);
            input.setHomeNetworkPublicKey("aabbccdd");
            input.setHomeNetworkPrivateKey(PROFILE_A_PRIV_KEY);

            assertThatThrownBy(() -> service.create(TENANT, input))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("32 bytes");
        }

        @Test
        void create_profileB_wrongPublicKeyLength_throwsBadRequest() {
            SuciProfile input = new SuciProfile();
            input.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_B);
            input.setHomeNetworkPublicKeyId(2);
            input.setHomeNetworkPublicKey(PROFILE_A_PUB_KEY);
            input.setHomeNetworkPrivateKey(PROFILE_B_PRIV_KEY);

            assertThatThrownBy(() -> service.create(TENANT, input))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("33 bytes");
        }

        @Test
        void create_profileB_validKey_succeeds() {
            when(repository.findByTenantIdAndHomeNetworkPublicKeyIdAndProtectionScheme(
                    TENANT, 2, SuciProfile.ProtectionScheme.PROFILE_B))
                    .thenReturn(Optional.empty());
            when(encryptionService.encrypt(anyString())).thenReturn(ENCRYPTED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SuciProfile input = new SuciProfile();
            input.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_B);
            input.setHomeNetworkPublicKeyId(2);
            input.setHomeNetworkPublicKey(PROFILE_B_PUB_KEY);
            input.setHomeNetworkPrivateKey(PROFILE_B_PRIV_KEY);

            SuciProfile result = service.create(TENANT, input);

            assertThat(result.getProtectionScheme()).isEqualTo(SuciProfile.ProtectionScheme.PROFILE_B);
        }

        @Test
        void create_nullScheme_skipsKeyValidation() {
            when(repository.findByTenantIdAndHomeNetworkPublicKeyIdAndProtectionScheme(
                    TENANT, 0, SuciProfile.ProtectionScheme.NULL_SCHEME))
                    .thenReturn(Optional.empty());
            when(encryptionService.encrypt(anyString())).thenReturn(ENCRYPTED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SuciProfile input = new SuciProfile();
            input.setProtectionScheme(SuciProfile.ProtectionScheme.NULL_SCHEME);
            input.setHomeNetworkPublicKeyId(0);
            input.setHomeNetworkPublicKey("anything");
            input.setHomeNetworkPrivateKey("anything");

            SuciProfile result = service.create(TENANT, input);

            assertThat(result.getProtectionScheme()).isEqualTo(SuciProfile.ProtectionScheme.NULL_SCHEME);
        }
    }

    // ── Read ────────────────────────────────────────────────────────────

    @Nested
    class Read {

        @Test
        void getById_found_masksPrivateKey() {
            sampleProfile.setHomeNetworkPrivateKey(ENCRYPTED);
            when(repository.findByIdAndTenantId("suci-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));

            SuciProfile result = service.getById(TENANT, "suci-1");

            assertThat(result.getId()).isEqualTo("suci-1");
            assertThat(result.getHomeNetworkPrivateKey()).isNull();
        }

        @Test
        void getById_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(TENANT, "missing"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        void list_masksAllPrivateKeys() {
            sampleProfile.setHomeNetworkPrivateKey(ENCRYPTED);
            Pageable pageable = PageRequest.of(0, 20);
            when(repository.findByTenantId(TENANT, pageable))
                    .thenReturn(new PageImpl<>(List.of(sampleProfile)));

            Page<SuciProfile> result = service.list(TENANT, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getHomeNetworkPrivateKey()).isNull();
        }

        @Test
        void listByStatus_delegatesAndMasksKeys() {
            sampleProfile.setHomeNetworkPrivateKey(ENCRYPTED);
            when(repository.findByTenantIdAndKeyStatus(TENANT, SuciProfile.KeyStatus.ACTIVE))
                    .thenReturn(List.of(sampleProfile));

            List<SuciProfile> result = service.listByStatus(TENANT, SuciProfile.KeyStatus.ACTIVE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getHomeNetworkPrivateKey()).isNull();
        }

        @Test
        void listByScheme_delegatesAndMasksKeys() {
            sampleProfile.setHomeNetworkPrivateKey(ENCRYPTED);
            when(repository.findByTenantIdAndProtectionScheme(TENANT, SuciProfile.ProtectionScheme.PROFILE_A))
                    .thenReturn(List.of(sampleProfile));

            List<SuciProfile> result = service.listByScheme(TENANT, SuciProfile.ProtectionScheme.PROFILE_A);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getHomeNetworkPrivateKey()).isNull();
        }
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Nested
    class Update {

        @Test
        void update_activeProfile_updatesFieldsAndEncrypts() {
            when(repository.findByIdAndTenantId("suci-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));
            when(repository.findByTenantIdAndHomeNetworkPublicKeyIdAndProtectionScheme(
                    TENANT, 1, SuciProfile.ProtectionScheme.PROFILE_A))
                    .thenReturn(Optional.of(sampleProfile));
            when(encryptionService.encrypt(PROFILE_A_PRIV_KEY)).thenReturn(ENCRYPTED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SuciProfile updated = new SuciProfile();
            updated.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_A);
            updated.setHomeNetworkPublicKeyId(1);
            updated.setHomeNetworkPublicKey(PROFILE_A_PUB_KEY);
            updated.setHomeNetworkPrivateKey(PROFILE_A_PRIV_KEY);
            updated.setDescription("Updated description");

            SuciProfile result = service.update(TENANT, "suci-1", updated);

            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getHomeNetworkPrivateKey()).isEqualTo(ENCRYPTED);
        }

        @Test
        void update_revokedProfile_throwsConflict() {
            sampleProfile.setKeyStatus(SuciProfile.KeyStatus.REVOKED);
            when(repository.findByIdAndTenantId("suci-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));

            SuciProfile updated = new SuciProfile();
            updated.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_A);
            updated.setHomeNetworkPublicKeyId(1);
            updated.setHomeNetworkPublicKey(PROFILE_A_PUB_KEY);
            updated.setHomeNetworkPrivateKey(PROFILE_A_PRIV_KEY);

            assertThatThrownBy(() -> service.update(TENANT, "suci-1", updated))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("revoked");
        }

        @Test
        void update_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TENANT, "missing", new SuciProfile()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Nested
    class Delete {

        @Test
        void delete_found_deletesProfile() {
            when(repository.findByIdAndTenantId("suci-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));

            service.delete(TENANT, "suci-1");

            verify(repository).delete(sampleProfile);
        }

        @Test
        void delete_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(TENANT, "missing"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── Revoke ──────────────────────────────────────────────────────────

    @Nested
    class Revoke {

        @Test
        void revokeKey_activeProfile_setsRevoked() {
            when(repository.findByIdAndTenantId("suci-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SuciProfile result = service.revokeKey(TENANT, "suci-1");

            assertThat(result.getKeyStatus()).isEqualTo(SuciProfile.KeyStatus.REVOKED);
        }

        @Test
        void revokeKey_alreadyRevoked_throwsConflict() {
            sampleProfile.setKeyStatus(SuciProfile.KeyStatus.REVOKED);
            when(repository.findByIdAndTenantId("suci-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));

            assertThatThrownBy(() -> service.revokeKey(TENANT, "suci-1"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already revoked");
        }

        @Test
        void revokeKey_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revokeKey(TENANT, "missing"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── Count ───────────────────────────────────────────────────────────

    @Nested
    class Count {

        @Test
        void count_delegatesToRepository() {
            when(repository.countByTenantId(TENANT)).thenReturn(5L);
            assertThat(service.count(TENANT)).isEqualTo(5);
        }

        @Test
        void countByStatus_delegatesToRepository() {
            when(repository.countByTenantIdAndKeyStatus(TENANT, SuciProfile.KeyStatus.ACTIVE))
                    .thenReturn(3L);
            assertThat(service.countByStatus(TENANT, SuciProfile.KeyStatus.ACTIVE)).isEqualTo(3);
        }
    }
}
