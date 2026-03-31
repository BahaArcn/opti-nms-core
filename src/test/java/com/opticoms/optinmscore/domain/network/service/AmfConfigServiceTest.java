package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.repository.AmfConfigRepository;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmfConfigServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private AmfConfigRepository repo;
    @Mock private GlobalConfigRepository globalConfigRepository;
    @InjectMocks private AmfConfigService service;

    private AmfConfig config;

    @BeforeEach
    void setUp() {
        config = buildFullConfig();
    }

    @Test
    void saveOrUpdate_new_setsAndSaves() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_5G);
        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(AmfConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AmfConfig saved = service.saveOrUpdateAmfConfig(TENANT, config);

        assertEquals(TENANT, saved.getTenantId());
        assertEquals("test-amf", saved.getAmfName());
        verify(repo).save(config);
    }

    @Test
    void saveOrUpdate_existing_preservesMetadata() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_5G);
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
    void saveOrUpdate_tacEndLessThanTac_throws() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_5G);

        AmfConfig.Plmn plmn = new AmfConfig.Plmn();
        plmn.setMcc("999");
        plmn.setMnc("70");

        AmfConfig.Tai tai = new AmfConfig.Tai();
        tai.setPlmn(plmn);
        tai.setTac(100);
        tai.setTacEnd(50);
        config.setSupportedTais(List.of(tai));

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateAmfConfig(TENANT, config));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("tacEnd (50) must be >= tac (100)"));
        verify(repo, never()).save(any());
    }

    @Test
    void saveOrUpdate_tacEndZero_noValidationError() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_5G);

        AmfConfig.Plmn plmn = new AmfConfig.Plmn();
        plmn.setMcc("999");
        plmn.setMnc("70");

        AmfConfig.Tai tai = new AmfConfig.Tai();
        tai.setPlmn(plmn);
        tai.setTac(100);
        tai.setTacEnd(0);
        config.setSupportedTais(List.of(tai));

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(AmfConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AmfConfig saved = service.saveOrUpdateAmfConfig(TENANT, config);

        assertNotNull(saved);
        verify(repo).save(config);
    }

    @Test
    void saveOrUpdate_tacEndEqualsTac_noValidationError() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_5G);

        AmfConfig.Plmn plmn = new AmfConfig.Plmn();
        plmn.setMcc("999");
        plmn.setMnc("70");

        AmfConfig.Tai tai = new AmfConfig.Tai();
        tai.setPlmn(plmn);
        tai.setTac(100);
        tai.setTacEnd(100);
        config.setSupportedTais(List.of(tai));

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(AmfConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AmfConfig saved = service.saveOrUpdateAmfConfig(TENANT, config);

        assertNotNull(saved);
        verify(repo).save(config);
    }

    @Test
    void saveOrUpdate_nullSupportedTais_noValidationError() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_5G);
        config.setSupportedTais(null);

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(AmfConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AmfConfig saved = service.saveOrUpdateAmfConfig(TENANT, config);

        assertNotNull(saved);
        verify(repo).save(config);
    }

    @Test
    void saveOrUpdate_only5gMode_missingAmfName_throws() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_5G);
        config.setAmfName(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateAmfConfig(TENANT, config));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("amfName"));
    }

    @Test
    void saveOrUpdate_only5gMode_missingAmfId_throws() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_5G);
        config.setAmfId(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateAmfConfig(TENANT, config));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("amfId"));
    }

    @Test
    void saveOrUpdate_only5gMode_missingSlices_throws() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_5G);
        config.setSupportedSlices(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateAmfConfig(TENANT, config));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("supportedSlices"));
    }

    @Test
    void saveOrUpdate_only4gMode_missingMmeName_throws() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_4G);
        config.setMmeName(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateAmfConfig(TENANT, config));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("mmeName"));
    }

    @Test
    void saveOrUpdate_only4gMode_missingMmeId_throws() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_4G);
        config.setMmeId(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateAmfConfig(TENANT, config));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("mmeId"));
    }

    @Test
    void saveOrUpdate_only4gMode_no5gFields_ok() {
        stubGlobalMode(GlobalConfig.NetworkMode.ONLY_4G);
        config.setAmfName(null);
        config.setAmfId(null);
        config.setSupportedSlices(null);
        config.setNasTimers5g(null);

        AmfConfig.MmeId mmeId = new AmfConfig.MmeId();
        mmeId.setMmegi(2);
        mmeId.setMmec(1);
        config.setMmeId(mmeId);
        config.setMmeName("test-mme");

        when(repo.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(repo.save(any(AmfConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AmfConfig saved = service.saveOrUpdateAmfConfig(TENANT, config);
        assertNotNull(saved);
    }

    @Test
    void saveOrUpdate_hybridMode_requiresBoth() {
        stubGlobalMode(GlobalConfig.NetworkMode.HYBRID_4G_5G);
        config.setMmeName(null);
        config.setMmeId(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.saveOrUpdateAmfConfig(TENANT, config));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("mmeName"));
        assertTrue(ex.getReason().contains("mmeId"));
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

    private void stubGlobalMode(GlobalConfig.NetworkMode mode) {
        GlobalConfig global = new GlobalConfig();
        global.setNetworkMode(mode);
        when(globalConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.of(global));
    }

    private AmfConfig buildFullConfig() {
        AmfConfig c = new AmfConfig();
        c.setAmfName("test-amf");

        AmfConfig.AmfId amfId = new AmfConfig.AmfId();
        amfId.setRegion(2);
        amfId.setSet(1);
        amfId.setPointer(0);
        c.setAmfId(amfId);

        AmfConfig.MmeId mmeId = new AmfConfig.MmeId();
        mmeId.setMmegi(2);
        mmeId.setMmec(1);
        c.setMmeId(mmeId);
        c.setMmeName("test-mme");

        AmfConfig.Plmn plmn = new AmfConfig.Plmn();
        plmn.setMcc("999");
        plmn.setMnc("70");
        c.setSupportedPlmns(List.of(plmn));

        AmfConfig.Tai tai = new AmfConfig.Tai();
        tai.setPlmn(plmn);
        tai.setTac(1);
        c.setSupportedTais(List.of(tai));

        AmfConfig.Slice slice = new AmfConfig.Slice();
        slice.setSst(1);
        c.setSupportedSlices(List.of(slice));

        c.setN2InterfaceIp("0.0.0.0");
        c.setSecurityParameters(new AmfConfig.SecurityParameters());
        c.setNasTimers5g(new AmfConfig.NasTimers5g());
        c.setNasTimers4g(new AmfConfig.NasTimers4g());
        return c;
    }
}
