package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.network.model.*;
import com.opticoms.optinmscore.domain.network.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkConfigServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private GlobalConfigRepository repo;
    @InjectMocks private NetworkConfigService service;

    private GlobalConfig config;

    @BeforeEach
    void setUp() {
        config = new GlobalConfig();
        config.setNetworkFullName("Test Network");
        config.setNetworkShortName("TST");
        config.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);
    }

    @Test
    void saveOrUpdate_newConfig_setsAndSaves() {
        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(GlobalConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        GlobalConfig saved = service.saveOrUpdateGlobalConfig(TENANT, config);

        assertEquals(TENANT, saved.getTenantId());
        assertEquals("Test Network", saved.getNetworkFullName());
        verify(repo).save(config);
    }

    @Test
    void saveOrUpdate_existingConfig_preservesIdAndVersion() {
        GlobalConfig existing = new GlobalConfig();
        existing.setId("existing-id");
        existing.setVersion(3L);
        existing.setCreatedAt(1000L);
        existing.setCreatedBy("admin");
        existing.setMaxSupportedDevices(2048);
        existing.setMaxSupportedGNBs(128);

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.of(existing));
        when(repo.save(any(GlobalConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        GlobalConfig saved = service.saveOrUpdateGlobalConfig(TENANT, config);

        assertEquals("existing-id", saved.getId());
        assertEquals(3L, saved.getVersion());
        assertEquals(1000L, saved.getCreatedAt());
        assertEquals(2048, saved.getMaxSupportedDevices(), "maxSupportedDevices should be preserved");
        assertEquals(128, saved.getMaxSupportedGNBs(), "maxSupportedGNBs should be preserved");
    }

    @Test
    void getGlobalConfig_found_returnsConfig() {
        when(repo.findByTenantId(TENANT)).thenReturn(Optional.of(config));

        GlobalConfig result = service.getGlobalConfig(TENANT);

        assertNotNull(result);
        assertEquals("Test Network", result.getNetworkFullName());
    }

    @Test
    void getGlobalConfig_notFound_throwsException() {
        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getGlobalConfig(TENANT));
        assertTrue(ex.getMessage().contains(TENANT));
    }
}
