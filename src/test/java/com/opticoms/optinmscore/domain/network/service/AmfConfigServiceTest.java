package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import com.opticoms.optinmscore.domain.network.repository.AmfConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmfConfigServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private AmfConfigRepository repo;
    @InjectMocks private AmfConfigService service;

    private AmfConfig config;

    @BeforeEach
    void setUp() {
        config = new AmfConfig();
        config.setAmfName("test-amf");

        AmfConfig.AmfId amfId = new AmfConfig.AmfId();
        amfId.setRegion(2);
        amfId.setSet(1);
        amfId.setPointer(0);
        config.setAmfId(amfId);

        AmfConfig.Plmn plmn = new AmfConfig.Plmn();
        plmn.setMcc("999");
        plmn.setMnc("70");
        config.setSupportedPlmns(List.of(plmn));

        AmfConfig.Tai tai = new AmfConfig.Tai();
        tai.setPlmn(plmn);
        tai.setTac(1);
        config.setSupportedTais(List.of(tai));

        AmfConfig.Slice slice = new AmfConfig.Slice();
        slice.setSst(1);
        config.setSupportedSlices(List.of(slice));

        config.setN2InterfaceIp("0.0.0.0");
        config.setSecurityParameters(new AmfConfig.SecurityParameters());
        config.setNasTimers5g(new AmfConfig.NasTimers5g());
        config.setNasTimers4g(new AmfConfig.NasTimers4g());
    }

    @Test
    void saveOrUpdate_new_setsAndSaves() {
        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(AmfConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AmfConfig saved = service.saveOrUpdateAmfConfig(TENANT, config);

        assertEquals(TENANT, saved.getTenantId());
        assertEquals("test-amf", saved.getAmfName());
        verify(repo).save(config);
    }

    @Test
    void saveOrUpdate_existing_preservesMetadata() {
        AmfConfig existing = new AmfConfig();
        existing.setId("existing-id");
        existing.setVersion(5L);
        existing.setCreatedAt(2000L);
        existing.setCreatedBy("admin");

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.of(existing));
        when(repo.save(any(AmfConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AmfConfig saved = service.saveOrUpdateAmfConfig(TENANT, config);

        assertEquals("existing-id", saved.getId());
        assertEquals(5L, saved.getVersion());
        assertEquals(2000L, saved.getCreatedAt());
        assertEquals("admin", saved.getCreatedBy());
    }

    @Test
    void getAmfConfig_found() {
        when(repo.findByTenantId(TENANT)).thenReturn(Optional.of(config));
        AmfConfig result = service.getAmfConfig(TENANT);
        assertEquals("test-amf", result.getAmfName());
    }

    @Test
    void getAmfConfig_notFound_throws() {
        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getAmfConfig(TENANT));
    }
}
