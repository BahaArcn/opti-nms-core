package com.opticoms.optinmscore.integration.open5gs.deploy.service;

import com.opticoms.optinmscore.domain.network.model.*;
import com.opticoms.optinmscore.domain.network.service.*;
import com.opticoms.optinmscore.integration.open5gs.deploy.dto.RenderedConfigs;
import com.opticoms.optinmscore.integration.open5gs.deploy.renderer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigRenderServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private NetworkConfigService networkConfigService;
    @Mock private AmfConfigService amfConfigService;
    @Mock private SmfConfigService smfConfigService;
    @Mock private UpfConfigService upfConfigService;

    @Spy private AmfYamlRenderer amfRenderer = new AmfYamlRenderer();
    @Spy private SmfYamlRenderer smfRenderer = new SmfYamlRenderer();
    @Spy private UpfYamlRenderer upfRenderer = new UpfYamlRenderer();
    @Spy private NrfYamlRenderer nrfRenderer = new NrfYamlRenderer();
    @Spy private NssfYamlRenderer nssfRenderer = new NssfYamlRenderer();
    @Spy private CommonNfYamlRenderer commonNfRenderer = new CommonNfYamlRenderer();
    @Spy private MmeYamlRenderer mmeRenderer = new MmeYamlRenderer();
    @Spy private SgwuYamlRenderer sgwuRenderer = new SgwuYamlRenderer();

    @InjectMocks
    private ConfigRenderService service;

    private GlobalConfig global;
    private AmfConfig amf;
    private SmfConfig smf;
    private UpfConfig upf;

    @BeforeEach
    void setUp() {
        global = buildGlobal(GlobalConfig.NetworkMode.ONLY_5G);
        amf = buildAmf();
        smf = buildSmf();
        upf = buildUpf();
    }

    @Test
    void renderAll_5gMode_producesAllYamls_noMme() {
        stubAll();
        RenderedConfigs result = service.renderAll(TENANT);

        assertAll(
                () -> assertNotNull(result.getAmfYaml(), "amfYaml"),
                () -> assertNotNull(result.getSmfYaml(), "smfYaml"),
                () -> assertNotNull(result.getUpfYaml(), "upfYaml"),
                () -> assertNotNull(result.getWrapperSh(), "wrapperSh"),
                () -> assertNotNull(result.getNrfYaml(), "nrfYaml"),
                () -> assertNotNull(result.getNssfYaml(), "nssfYaml"),
                () -> assertNotNull(result.getCommonNfYamls(), "commonNfYamls"),
                () -> assertNull(result.getMmeYaml(), "mmeYaml should be null in ONLY_5G mode")
        );

        assertTrue(result.getCommonNfYamls().containsKey("ausf"), "ausf in commonNfYamls");
        assertTrue(result.getCommonNfYamls().containsKey("udm"), "udm in commonNfYamls");
        assertTrue(result.getCommonNfYamls().containsKey("udr"), "udr in commonNfYamls");
    }

    @Test
    void renderAll_hybridMode_includesMme() {
        global.setNetworkMode(GlobalConfig.NetworkMode.HYBRID_4G_5G);
        amf.setMmeName("open5gs-mme0");
        amf.setS1cInterfaceIp("10.10.1.2");
        AmfConfig.MmeId mmeId = new AmfConfig.MmeId();
        mmeId.setMmegi(32768);
        mmeId.setMmec(1);
        amf.setMmeId(mmeId);

        stubAll();
        RenderedConfigs result = service.renderAll(TENANT);

        assertNotNull(result.getMmeYaml(), "mmeYaml should exist in HYBRID mode");
        assertTrue(result.getMmeYaml().contains("mme_name: open5gs-mme0"));
    }

    @Test
    void renderAll_4gOnlyMode_includesMme() {
        global.setNetworkMode(GlobalConfig.NetworkMode.ONLY_4G);
        amf.setMmeName("mme-test");
        amf.setS1cInterfaceIp("192.168.1.1");
        AmfConfig.MmeId mmeId = new AmfConfig.MmeId();
        mmeId.setMmegi(1);
        mmeId.setMmec(1);
        amf.setMmeId(mmeId);

        stubAll();
        RenderedConfigs result = service.renderAll(TENANT);

        assertNotNull(result.getMmeYaml());
        assertTrue(result.getMmeYaml().contains("mme_name: mme-test"));
    }

    @Test
    void renderAmfOnly_returnsOnlyAmfYaml() {
        when(networkConfigService.getGlobalConfig(TENANT)).thenReturn(global);
        when(amfConfigService.getAmfConfig(TENANT)).thenReturn(amf);

        RenderedConfigs result = service.renderAmfOnly(TENANT);

        assertNotNull(result.getAmfYaml());
        assertNull(result.getSmfYaml());
        assertNull(result.getUpfYaml());
        verify(smfConfigService, never()).getSmfConfig(any());
    }

    @Test
    void renderSmfOnly_returnsOnlySmfYaml() {
        when(networkConfigService.getGlobalConfig(TENANT)).thenReturn(global);
        when(smfConfigService.getSmfConfig(TENANT)).thenReturn(smf);
        when(upfConfigService.getUpfConfig(TENANT)).thenReturn(upf);

        RenderedConfigs result = service.renderSmfOnly(TENANT);

        assertNotNull(result.getSmfYaml());
        assertNull(result.getAmfYaml());
        verify(amfConfigService, never()).getAmfConfig(any());
    }

    @Test
    void renderUpfOnly_returnsUpfYamlAndWrapperSh() {
        when(networkConfigService.getGlobalConfig(TENANT)).thenReturn(global);
        when(smfConfigService.getSmfConfig(TENANT)).thenReturn(smf);
        when(upfConfigService.getUpfConfig(TENANT)).thenReturn(upf);

        RenderedConfigs result = service.renderUpfOnly(TENANT);

        assertNotNull(result.getUpfYaml());
        assertNotNull(result.getWrapperSh());
        assertNull(result.getAmfYaml());
    }

    @Test
    void renderAll_dbServiceCalledExactlyOnce() {
        stubAll();
        service.renderAll(TENANT);

        verify(networkConfigService, times(1)).getGlobalConfig(TENANT);
        verify(amfConfigService, times(1)).getAmfConfig(TENANT);
        verify(smfConfigService, times(1)).getSmfConfig(TENANT);
        verify(upfConfigService, times(1)).getUpfConfig(TENANT);
    }

    @Test
    void renderAll_yamlContentQuality() {
        stubAll();
        RenderedConfigs result = service.renderAll(TENANT);

        assertAll("YAML content quality",
                () -> assertTrue(result.getAmfYaml().contains("amf_name:"), "AMF has amf_name"),
                () -> assertTrue(result.getSmfYaml().contains("pfcp:"), "SMF has pfcp"),
                () -> assertTrue(result.getSmfYaml().contains("freeDiameter:"), "SMF has freeDiameter"),
                () -> assertTrue(result.getUpfYaml().contains("gtpu:"), "UPF has gtpu"),
                () -> assertTrue(result.getWrapperSh().contains("#!/bin/bash"), "wrapper.sh has shebang"),
                () -> assertTrue(result.getNrfYaml().contains("serving:"), "NRF has serving"),
                () -> assertTrue(result.getNssfYaml().contains("nsi:"), "NSSF has nsi"),
                () -> assertTrue(result.getCommonNfYamls().get("udr").contains("db_uri:"), "UDR has db_uri"),
                () -> assertTrue(result.getCommonNfYamls().get("udm").contains("hnet:"), "UDM has hnet")
        );
    }

    private void stubAll() {
        when(networkConfigService.getGlobalConfig(TENANT)).thenReturn(global);
        when(amfConfigService.getAmfConfig(TENANT)).thenReturn(amf);
        when(smfConfigService.getSmfConfig(TENANT)).thenReturn(smf);
        when(upfConfigService.getUpfConfig(TENANT)).thenReturn(upf);
    }

    // ─── Test data builders ────────────────────────────────────────────

    private GlobalConfig buildGlobal(GlobalConfig.NetworkMode mode) {
        GlobalConfig g = new GlobalConfig();
        g.setNetworkFullName("Test 5G Network");
        g.setNetworkShortName("TST5G");
        g.setNetworkMode(mode);
        g.setMaxSupportedDevices(512);
        g.setMaxSupportedGNBs(32);
        return g;
    }

    private AmfConfig buildAmf() {
        AmfConfig a = new AmfConfig();
        a.setAmfName("test-amf");

        AmfConfig.AmfId id = new AmfConfig.AmfId();
        id.setRegion(1);
        id.setSet(1);
        id.setPointer(0);
        a.setAmfId(id);

        AmfConfig.Plmn plmn = new AmfConfig.Plmn();
        plmn.setMcc("001");
        plmn.setMnc("01");
        a.setSupportedPlmns(List.of(plmn));

        AmfConfig.Tai tai = new AmfConfig.Tai();
        tai.setPlmn(plmn);
        tai.setTac(7);
        a.setSupportedTais(List.of(tai));

        AmfConfig.Slice slice = new AmfConfig.Slice();
        slice.setSst(1);
        slice.setSd("000001");
        a.setSupportedSlices(List.of(slice));

        a.setN2InterfaceIp("0.0.0.0");
        a.setSecurityParameters(new AmfConfig.SecurityParameters());
        a.setNasTimers5g(new AmfConfig.NasTimers5g());
        a.setNasTimers4g(new AmfConfig.NasTimers4g());
        return a;
    }

    private SmfConfig buildSmf() {
        SmfConfig s = new SmfConfig();
        s.setMtu(1400);
        s.setDnsIps(List.of("8.8.8.8"));
        s.setSecurityIndication(new SmfConfig.SecurityIndication());

        SmfConfig.ApnDnn dnn = new SmfConfig.ApnDnn();
        dnn.setApnDnnName("internet");
        dnn.setUeIpRange("10.45.0.0/16");
        dnn.setGatewayIp("10.45.0.1");
        dnn.setLocal(true);

        SmfConfig.SliceId sid = new SmfConfig.SliceId();
        sid.setSst(1);
        sid.setSd("000001");
        dnn.setSliceId(sid);

        SmfConfig.Tai tai = new SmfConfig.Tai();
        SmfConfig.Plmn plmn = new SmfConfig.Plmn();
        plmn.setMcc("001");
        plmn.setMnc("01");
        tai.setPlmn(plmn);
        tai.setTac(7);
        dnn.setTai(tai);

        s.setApnList(new ArrayList<>(List.of(dnn)));
        return s;
    }

    private UpfConfig buildUpf() {
        UpfConfig u = new UpfConfig();
        u.setN3InterfaceIp("10.11.0.6");
        u.setN4PfcpIp("10.10.4.1");
        return u;
    }
}
