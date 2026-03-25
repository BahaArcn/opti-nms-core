package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import com.opticoms.optinmscore.domain.network.repository.UpfConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpfConfigServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private UpfConfigRepository upfConfigRepository;

    @InjectMocks
    private UpfConfigService service;

    @Test
    void saveOrUpdateUpfConfig_newConfig_setsTenantAndSaves() {
        when(upfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(upfConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpfConfig config = buildUpfConfig();
        UpfConfig result = service.saveOrUpdateUpfConfig(TENANT, config);

        assertEquals(TENANT, result.getTenantId());
        verify(upfConfigRepository).save(config);
    }

    @Test
    void saveOrUpdateUpfConfig_existingConfig_preservesMetadata() {
        UpfConfig existing = buildUpfConfig();
        existing.setId("existing-id");
        existing.setVersion(3L);
        long createdAt = System.currentTimeMillis() - 3600000;
        existing.setCreatedAt(createdAt);
        existing.setCreatedBy("admin");

        when(upfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.of(existing));
        when(upfConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpfConfig newConfig = buildUpfConfig();
        newConfig.setN3InterfaceIp("10.45.0.99");
        UpfConfig result = service.saveOrUpdateUpfConfig(TENANT, newConfig);

        assertEquals("existing-id", result.getId());
        assertEquals(3L, result.getVersion());
        assertEquals(createdAt, result.getCreatedAt());
        assertEquals("admin", result.getCreatedBy());
        assertEquals("10.45.0.99", result.getN3InterfaceIp());
    }

    @Test
    void getUpfConfig_found() {
        UpfConfig config = buildUpfConfig();
        when(upfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.of(config));

        UpfConfig result = service.getUpfConfig(TENANT);

        assertEquals(config, result);
    }

    @Test
    void getUpfConfig_notFound_throwsRuntime() {
        when(upfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getUpfConfig(TENANT));
        assertTrue(ex.getMessage().contains("UPF Configuration not found"));
    }

    private UpfConfig buildUpfConfig() {
        UpfConfig c = new UpfConfig();
        c.setN3InterfaceIp("10.45.0.1");
        c.setS1uInterfaceIp("10.45.0.2");
        c.setN4PfcpIp("10.10.4.1");
        return c;
    }
}
