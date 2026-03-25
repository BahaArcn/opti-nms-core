package com.opticoms.optinmscore.domain.apn.service;

import com.opticoms.optinmscore.domain.apn.model.ApnProfile;
import com.opticoms.optinmscore.domain.apn.repository.ApnProfileRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApnProfileServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private ApnProfileRepository repository;
    @InjectMocks private ApnProfileService service;

    private ApnProfile sampleProfile;

    @BeforeEach
    void setUp() {
        sampleProfile = buildProfile("internet", 1);
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Nested
    class Create {

        @Test
        void create_valid_setsTenantAndSaves() {
            when(repository.findByTenantIdAndDnnAndSst(TENANT, "internet", 1))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApnProfile input = buildProfile("internet", 1);
            ApnProfile result = service.create(TENANT, input);

            assertThat(result.getTenantId()).isEqualTo(TENANT);
            assertThat(result.getDnn()).isEqualTo("internet");

            ArgumentCaptor<ApnProfile> captor = ArgumentCaptor.forClass(ApnProfile.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT);
        }

        @Test
        void create_duplicateDnnSst_throwsConflict() {
            ApnProfile existing = new ApnProfile();
            existing.setId("other-id");
            when(repository.findByTenantIdAndDnnAndSst(TENANT, "internet", 1))
                    .thenReturn(Optional.of(existing));

            ApnProfile input = buildProfile("internet", 1);

            assertThatThrownBy(() -> service.create(TENANT, input))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        void create_invalidSd_throwsBadRequest() {
            ApnProfile input = buildProfile("internet", 1);
            input.setSd("ZZZZZZ");

            assertThatThrownBy(() -> service.create(TENANT, input))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("SD must be");
        }

        @Test
        void create_validSd_succeeds() {
            when(repository.findByTenantIdAndDnnAndSst(TENANT, "ims", 2))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApnProfile input = buildProfile("ims", 2);
            input.setSd("000001");

            ApnProfile result = service.create(TENANT, input);

            assertThat(result.getSd()).isEqualTo("000001");
        }

        @Test
        void create_nullSd_skipsValidation() {
            when(repository.findByTenantIdAndDnnAndSst(TENANT, "internet", 1))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApnProfile input = buildProfile("internet", 1);
            input.setSd(null);

            ApnProfile result = service.create(TENANT, input);

            assertThat(result.getSd()).isNull();
        }
    }

    // ── Read ────────────────────────────────────────────────────────────

    @Nested
    class Read {

        @Test
        void getById_found_returnsProfile() {
            when(repository.findByIdAndTenantId("apn-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));

            ApnProfile result = service.getById(TENANT, "apn-1");

            assertThat(result.getDnn()).isEqualTo("internet");
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
        void list_delegatesToRepository() {
            Pageable pageable = PageRequest.of(0, 20);
            when(repository.findByTenantId(TENANT, pageable))
                    .thenReturn(new PageImpl<>(List.of(sampleProfile)));

            Page<ApnProfile> result = service.list(TENANT, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void listByStatus_delegatesCorrectly() {
            when(repository.findByTenantIdAndStatus(TENANT, ApnProfile.ProfileStatus.ACTIVE))
                    .thenReturn(List.of(sampleProfile));

            assertThat(service.listByStatus(TENANT, ApnProfile.ProfileStatus.ACTIVE)).hasSize(1);
        }

        @Test
        void listBySst_delegatesCorrectly() {
            when(repository.findByTenantIdAndSst(TENANT, 1))
                    .thenReturn(List.of(sampleProfile));

            assertThat(service.listBySst(TENANT, 1)).hasSize(1);
        }

        @Test
        void listEnabled_delegatesCorrectly() {
            when(repository.findByTenantIdAndEnabledTrue(TENANT))
                    .thenReturn(List.of(sampleProfile));

            assertThat(service.listEnabled(TENANT)).hasSize(1);
        }
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Nested
    class Update {

        @Test
        void update_activeProfile_updatesFields() {
            when(repository.findByIdAndTenantId("apn-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));
            when(repository.findByTenantIdAndDnnAndSst(TENANT, "ims", 2))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApnProfile updated = buildProfile("ims", 2);
            updated.setDescription("Updated IMS");

            ApnProfile result = service.update(TENANT, "apn-1", updated);

            assertThat(result.getDnn()).isEqualTo("ims");
            assertThat(result.getSst()).isEqualTo(2);
            assertThat(result.getDescription()).isEqualTo("Updated IMS");
        }

        @Test
        void update_deprecatedProfile_throwsConflict() {
            sampleProfile.setStatus(ApnProfile.ProfileStatus.DEPRECATED);
            when(repository.findByIdAndTenantId("apn-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));

            assertThatThrownBy(() -> service.update(TENANT, "apn-1", new ApnProfile()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("deprecated");
        }

        @Test
        void update_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TENANT, "missing", new ApnProfile()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Nested
    class Delete {

        @Test
        void delete_found_deletesProfile() {
            when(repository.findByIdAndTenantId("apn-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));

            service.delete(TENANT, "apn-1");

            verify(repository).delete(sampleProfile);
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

    // ── Deprecate ───────────────────────────────────────────────────────

    @Nested
    class Deprecate {

        @Test
        void deprecate_activeProfile_setsDeprecatedAndDisabled() {
            when(repository.findByIdAndTenantId("apn-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApnProfile result = service.deprecate(TENANT, "apn-1");

            assertThat(result.getStatus()).isEqualTo(ApnProfile.ProfileStatus.DEPRECATED);
            assertThat(result.isEnabled()).isFalse();
        }

        @Test
        void deprecate_alreadyDeprecated_throwsConflict() {
            sampleProfile.setStatus(ApnProfile.ProfileStatus.DEPRECATED);
            when(repository.findByIdAndTenantId("apn-1", TENANT))
                    .thenReturn(Optional.of(sampleProfile));

            assertThatThrownBy(() -> service.deprecate(TENANT, "apn-1"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("already deprecated");
        }

        @Test
        void deprecate_notFound_throws404() {
            when(repository.findByIdAndTenantId("missing", TENANT))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deprecate(TENANT, "missing"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── Count ───────────────────────────────────────────────────────────

    @Nested
    class Count {

        @Test
        void count_delegatesToRepository() {
            when(repository.countByTenantId(TENANT)).thenReturn(10L);
            assertThat(service.count(TENANT)).isEqualTo(10);
        }

        @Test
        void countByStatus_delegatesToRepository() {
            when(repository.countByTenantIdAndStatus(TENANT, ApnProfile.ProfileStatus.ACTIVE))
                    .thenReturn(7L);
            assertThat(service.countByStatus(TENANT, ApnProfile.ProfileStatus.ACTIVE)).isEqualTo(7);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ApnProfile buildProfile(String dnn, int sst) {
        ApnProfile p = new ApnProfile();
        p.setId("apn-1");
        p.setTenantId(TENANT);
        p.setDnn(dnn);
        p.setSst(sst);
        p.setPduSessionType(ApnProfile.PduSessionType.IPV4);
        p.setEnabled(true);
        p.setStatus(ApnProfile.ProfileStatus.ACTIVE);
        p.setDescription("Test APN");

        ApnProfile.QosProfile qos = new ApnProfile.QosProfile();
        qos.setFiveQi(9);
        qos.setArpPriorityLevel(8);
        p.setQos(qos);

        ApnProfile.Ambr ambr = new ApnProfile.Ambr();
        ambr.setUplinkKbps(1000000L);
        ambr.setDownlinkKbps(2000000L);
        p.setSessionAmbr(ambr);

        return p;
    }
}
