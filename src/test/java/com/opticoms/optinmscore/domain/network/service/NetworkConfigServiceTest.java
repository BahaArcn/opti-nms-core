package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.network.model.*;
import com.opticoms.optinmscore.domain.network.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkConfigServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private GlobalConfigRepository repo;
    @Mock private AmfConfigRepository amfConfigRepository;
    @Mock private SmfConfigRepository smfConfigRepository;
    @Mock private UpfConfigRepository upfConfigRepository;
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
        existing.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);

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
    void saveOrUpdate_modeChange_incompatibleAmf_throwsConflict() {
        GlobalConfig existing = new GlobalConfig();
        existing.setId("id");
        existing.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);
        existing.setNetworkFullName("Test");
        existing.setNetworkShortName("T");

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.of(existing));

        AmfConfig amf = new AmfConfig();
        amf.setAmfName("amf");
        when(amfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.of(amf));
        when(upfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());

        GlobalConfig hybridConfig = new GlobalConfig();
        hybridConfig.setNetworkFullName("Test");
        hybridConfig.setNetworkShortName("T");
        hybridConfig.setNetworkMode(GlobalConfig.NetworkMode.HYBRID_4G_5G);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateGlobalConfig(TENANT, hybridConfig));
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("mmeName"));
    }

    @Test
    void saveOrUpdate_modeChange_compatible_saves() {
        GlobalConfig existing = new GlobalConfig();
        existing.setId("id");
        existing.setNetworkMode(GlobalConfig.NetworkMode.HYBRID_4G_5G);
        existing.setNetworkFullName("Test");
        existing.setNetworkShortName("T");

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.of(existing));
        when(amfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(upfConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(GlobalConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        GlobalConfig saved = service.saveOrUpdateGlobalConfig(TENANT, config);
        assertEquals(GlobalConfig.NetworkMode.ONLY_5G, saved.getNetworkMode());
    }

    @Test
    void saveOrUpdate_withValidPools_saves() {
        GlobalConfig.UeIpPool pool1 = new GlobalConfig.UeIpPool();
        pool1.setTunInterface("ogstun");
        pool1.setIpRange("10.45.0.0/16");
        pool1.setGatewayIp("10.45.0.1");

        GlobalConfig.UeIpPool pool2 = new GlobalConfig.UeIpPool();
        pool2.setTunInterface("ogstun2");
        pool2.setIpRange("10.46.0.0/16");
        pool2.setGatewayIp("10.46.0.1");

        config.setUeIpPoolList(new ArrayList<>(List.of(pool1, pool2)));

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(GlobalConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        GlobalConfig saved = service.saveOrUpdateGlobalConfig(TENANT, config);

        assertEquals(2, saved.getUeIpPoolList().size());
    }

    @Test
    void saveOrUpdate_duplicateTunInterface_throws400() {
        GlobalConfig.UeIpPool pool1 = new GlobalConfig.UeIpPool();
        pool1.setTunInterface("ogstun");
        pool1.setIpRange("10.45.0.0/16");
        pool1.setGatewayIp("10.45.0.1");

        GlobalConfig.UeIpPool pool2 = new GlobalConfig.UeIpPool();
        pool2.setTunInterface("ogstun");
        pool2.setIpRange("10.46.0.0/16");
        pool2.setGatewayIp("10.46.0.1");

        config.setUeIpPoolList(new ArrayList<>(List.of(pool1, pool2)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateGlobalConfig(TENANT, config));
        assertTrue(ex.getReason().contains("Duplicate tunInterface"));
    }

    @Test
    void saveOrUpdate_overlappingCidr_throws400() {
        GlobalConfig.UeIpPool pool1 = new GlobalConfig.UeIpPool();
        pool1.setTunInterface("ogstun");
        pool1.setIpRange("10.45.0.0/16");
        pool1.setGatewayIp("10.45.0.1");

        GlobalConfig.UeIpPool pool2 = new GlobalConfig.UeIpPool();
        pool2.setTunInterface("ogstun2");
        pool2.setIpRange("10.45.0.0/24");
        pool2.setGatewayIp("10.45.0.1");

        config.setUeIpPoolList(new ArrayList<>(List.of(pool1, pool2)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateGlobalConfig(TENANT, config));
        assertTrue(ex.getReason().contains("IP range overlap"));
    }

    @Test
    void saveOrUpdate_nonOverlappingCidr_saves() {
        GlobalConfig.UeIpPool pool1 = new GlobalConfig.UeIpPool();
        pool1.setTunInterface("ogstun");
        pool1.setIpRange("10.45.0.0/16");
        pool1.setGatewayIp("10.45.0.1");

        GlobalConfig.UeIpPool pool2 = new GlobalConfig.UeIpPool();
        pool2.setTunInterface("ogstun2");
        pool2.setIpRange("10.46.0.0/16");
        pool2.setGatewayIp("10.46.0.1");

        config.setUeIpPoolList(new ArrayList<>(List.of(pool1, pool2)));

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(GlobalConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.saveOrUpdateGlobalConfig(TENANT, config));
    }

    @Test
    void cidrOverlaps_sameNetwork_returnsTrue() {
        assertTrue(NetworkConfigService.cidrOverlaps("10.45.0.0/16", "10.45.0.0/24"));
    }

    @Test
    void cidrOverlaps_differentNetworks_returnsFalse() {
        assertFalse(NetworkConfigService.cidrOverlaps("10.45.0.0/16", "10.46.0.0/16"));
    }

    @Test
    void cidrOverlaps_rangeFormat_returnsFalse() {
        assertFalse(NetworkConfigService.cidrOverlaps("10.45.0.1-10.45.0.100", "10.45.0.0/16"));
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
