package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import com.opticoms.optinmscore.domain.network.repository.SmfConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmfConfigServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private SmfConfigRepository smfConfigRepository;

    @InjectMocks
    private SmfConfigService service;

    @Test
    void saveOrUpdateSmfConfig_newConfig_setsTenantAndSaves() {
        when(smfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(smfConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SmfConfig config = buildSmfConfig();
        SmfConfig result = service.saveOrUpdateSmfConfig(TENANT, config);

        assertEquals(TENANT, result.getTenantId());
        verify(smfConfigRepository).save(config);
    }

    @Test
    void saveOrUpdateSmfConfig_existingConfig_preservesMetadata() {
        SmfConfig existing = buildSmfConfig();
        existing.setId("existing-id");
        existing.setVersion(2L);
        long createdAt = System.currentTimeMillis() - 3600000;
        existing.setCreatedAt(createdAt);
        existing.setCreatedBy("admin");

        when(smfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.of(existing));
        when(smfConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SmfConfig newConfig = buildSmfConfig();
        newConfig.setMtu(1500);
        SmfConfig result = service.saveOrUpdateSmfConfig(TENANT, newConfig);

        assertEquals("existing-id", result.getId());
        assertEquals(2L, result.getVersion());
        assertEquals(createdAt, result.getCreatedAt());
        assertEquals("admin", result.getCreatedBy());
        assertEquals(1500, result.getMtu());
    }

    @Test
    void getSmfConfig_found() {
        SmfConfig config = buildSmfConfig();
        when(smfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.of(config));

        SmfConfig result = service.getSmfConfig(TENANT);

        assertEquals(config, result);
    }

    @Test
    void getSmfConfig_notFound_throwsRuntime() {
        when(smfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getSmfConfig(TENANT));
        assertTrue(ex.getMessage().contains("SMF Configuration not found"));
    }

    private SmfConfig buildSmfConfig() {
        SmfConfig c = new SmfConfig();
        c.setMtu(1400);
        c.setDnsIps(List.of("8.8.8.8", "8.8.4.4"));

        SmfConfig.SecurityIndication si = new SmfConfig.SecurityIndication();
        si.setIntegrity(SmfConfig.RequirementLevel.NOT_NEEDED);
        si.setCiphering(SmfConfig.RequirementLevel.NOT_NEEDED);
        c.setSecurityIndication(si);

        SmfConfig.ApnDnn apn = new SmfConfig.ApnDnn();
        apn.setApnDnnName("internet");
        apn.setUeIpRange("10.45.0.0/16");
        apn.setGatewayIp("10.45.0.1");
        c.setApnList(List.of(apn));

        return c;
    }
}
