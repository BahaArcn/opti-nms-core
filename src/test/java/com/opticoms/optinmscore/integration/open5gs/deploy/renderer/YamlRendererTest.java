package com.opticoms.optinmscore.integration.open5gs.deploy.renderer;

import com.opticoms.optinmscore.domain.network.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Renderer unit tests — no Spring context, no MongoDB required.
 * Verifies generated YAML strings contain expected Open5GS fields.
 */
class YamlRendererTest {

    private GlobalConfig global;
    private AmfConfig amf;
    private SmfConfig smf;
    private UpfConfig upf;

    @BeforeEach
    void setUp() {
        global = new GlobalConfig();
        global.setNetworkFullName("Opticoms 5G Network");
        global.setNetworkShortName("OPTC5G");
        global.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);
        global.setMaxSupportedDevices(1024);
        global.setMaxSupportedGNBs(64);

        GlobalConfig.UeIpPool pool1 = new GlobalConfig.UeIpPool();
        pool1.setTunInterface("ogstun");
        pool1.setIpRange("10.45.0.0/16");
        pool1.setGatewayIp("10.45.0.1");

        GlobalConfig.UeIpPool pool2 = new GlobalConfig.UeIpPool();
        pool2.setTunInterface("ogstun2");
        pool2.setIpRange("10.46.0.0/16");
        pool2.setGatewayIp("10.46.0.1");

        global.setUeIpPoolList(new ArrayList<>(List.of(pool1, pool2)));

        amf = new AmfConfig();
        amf.setAmfName("open5gs-amf0");

        AmfConfig.AmfId amfId = new AmfConfig.AmfId();
        amfId.setRegion(2);
        amfId.setSet(1);
        amfId.setPointer(0);
        amf.setAmfId(amfId);

        AmfConfig.Plmn plmn = new AmfConfig.Plmn();
        plmn.setMcc("999");
        plmn.setMnc("70");
        amf.setSupportedPlmns(List.of(plmn));

        AmfConfig.Tai tai = new AmfConfig.Tai();
        tai.setPlmn(plmn);
        tai.setTac(1);
        amf.setSupportedTais(List.of(tai));

        AmfConfig.Slice slice = new AmfConfig.Slice();
        slice.setSst(1);
        slice.setSd("FFFFFF");
        amf.setSupportedSlices(List.of(slice));

        amf.setN2InterfaceIp("0.0.0.0");
        amf.setSecurityParameters(new AmfConfig.SecurityParameters());
        amf.setNasTimers5g(new AmfConfig.NasTimers5g());
        amf.setNasTimers4g(new AmfConfig.NasTimers4g());

        smf = new SmfConfig();
        smf.setMtu(1400);
        smf.setDnsIps(List.of("8.8.8.8", "8.8.4.4"));
        smf.setSecurityIndication(new SmfConfig.SecurityIndication());

        SmfConfig.ApnDnn dnn1 = new SmfConfig.ApnDnn();
        dnn1.setApnDnnName("internet");
        dnn1.setTunInterface("ogstun");
        dnn1.setLocal(true);

        SmfConfig.SliceId sliceId = new SmfConfig.SliceId();
        sliceId.setSst(1);
        sliceId.setSd("FFFFFF");
        dnn1.setSliceId(sliceId);

        SmfConfig.Tai smfTai = new SmfConfig.Tai();
        SmfConfig.Plmn smfPlmn = new SmfConfig.Plmn();
        smfPlmn.setMcc("999");
        smfPlmn.setMnc("70");
        smfTai.setPlmn(smfPlmn);
        smfTai.setTac(1);
        dnn1.setTai(smfTai);

        SmfConfig.ApnDnn dnn2 = new SmfConfig.ApnDnn();
        dnn2.setApnDnnName("ims");
        dnn2.setTunInterface("ogstun2");
        dnn2.setLocal(true);
        dnn2.setSliceId(sliceId);
        dnn2.setTai(smfTai);

        smf.setApnList(new ArrayList<>(List.of(dnn1, dnn2)));

        upf = new UpfConfig();
        upf.setN3InterfaceIp("10.11.0.6");
        upf.setN4PfcpIp("10.10.4.1");
    }

    @Test
    void amfYaml_containsPointerAndNetworkName() {
        AmfYamlRenderer renderer = new AmfYamlRenderer();
        String yaml = renderer.render(global, amf);

        assertAll("amfcfg.yaml checks",
                () -> assertTrue(yaml.contains("amf_name: open5gs-amf0"), "amf_name"),
                () -> assertTrue(yaml.contains("pointer: 0"), "pointer in guami"),
                () -> assertTrue(yaml.contains("region: 2"), "region in guami"),
                () -> assertTrue(yaml.contains("set: 1"), "set in guami"),
                () -> assertTrue(yaml.contains("full: Opticoms 5G Network"), "network_name.full"),
                () -> assertTrue(yaml.contains("short: OPTC5G"), "network_name.short"),
                () -> assertTrue(yaml.contains("ue: 1024"), "global.max.ue"),
                () -> assertTrue(yaml.contains("gnb: 64"), "global.max.gnb"),
                () -> assertTrue(yaml.contains("mcc: 999"), "plmn mcc"),
                () -> assertTrue(yaml.contains("mnc: 70"), "plmn mnc"),
                () -> assertTrue(yaml.contains("tac: 1"), "tai tac"),
                () -> assertTrue(yaml.contains("sst: 1"), "slice sst"),
                () -> assertTrue(yaml.contains("t3502:"), "nas timer t3502"),
                () -> assertTrue(yaml.contains("t3512:"), "nas timer t3512"),
                () -> assertTrue(yaml.contains("NIA2"), "integrity order"),
                () -> assertTrue(yaml.contains("NEA0"), "ciphering order")
        );

        System.out.println("=== amfcfg.yaml ===");
        System.out.println(yaml);
    }

    @Test
    void smfYaml_containsFreeDiameterAndPfcp() {
        SmfYamlRenderer renderer = new SmfYamlRenderer();
        String yaml = renderer.render(smf, global, upf);

        assertAll("smfcfg.yaml checks",
                () -> assertTrue(yaml.contains("mtu: 1400"), "mtu"),
                () -> assertTrue(yaml.contains("freeDiameter: /open5gs/install/etc/freeDiameter/smf.conf"), "freeDiameter"),
                () -> assertTrue(yaml.contains("address: 10.10.4.1"), "pfcp client upf address from n4PfcpIp"),
                () -> assertTrue(yaml.contains("dnn: internet"), "dnn internet"),
                () -> assertTrue(yaml.contains("dnn: ims"), "dnn ims"),
                () -> assertTrue(yaml.contains("ue: 1024"), "global.max.ue"),
                () -> assertTrue(yaml.contains("gnb: 64"), "global.max.gnb"),
                () -> assertTrue(yaml.contains("8.8.8.8"), "dns1"),
                () -> assertTrue(yaml.contains("8.8.4.4"), "dns2"),
                () -> assertTrue(yaml.contains("10.45.0.1/16"), "subnet from pool1 gateway+prefix"),
                () -> assertTrue(yaml.contains("10.46.0.1/16"), "subnet from pool2 gateway+prefix")
        );

        System.out.println("=== smfcfg.yaml ===");
        System.out.println(yaml);
    }

    @Test
    void upfYaml_multiDnnTunNaming() {
        UpfYamlRenderer renderer = new UpfYamlRenderer();
        String yaml = renderer.renderYaml(upf, smf, global);

        assertAll("upfcfg.yaml checks",
                () -> assertTrue(yaml.contains("dev: ogstun"), "first DNN = ogstun"),
                () -> assertTrue(yaml.contains("dev: ogstun2"), "second DNN = ogstun2"),
                () -> assertTrue(yaml.contains("dnn: internet"), "dnn internet"),
                () -> assertTrue(yaml.contains("dnn: ims"), "dnn ims"),
                () -> assertTrue(yaml.contains("address: 10.11.0.6"), "n3 address"),
                () -> assertTrue(yaml.contains("ue: 1024"), "global.max.ue"),
                () -> assertTrue(yaml.contains("gnb: 64"), "global.max.gnb")
        );

        System.out.println("=== upfcfg.yaml ===");
        System.out.println(yaml);
    }

    @Test
    void wrapperSh_correctOrder() {
        UpfYamlRenderer renderer = new UpfYamlRenderer();
        String sh = renderer.renderWrapperScript(smf, global);

        int sysctlPos = sh.indexOf("sysctl -w net.ipv4.conf.all.rp_filter=0");
        int linkUpPos = sh.indexOf("ip link set ogstun up");
        int iptablesPos = sh.indexOf("iptables -t nat");
        int upfdPos = sh.indexOf("open5gs-upfd");

        assertAll("wrapper.sh order checks",
                () -> assertTrue(sh.contains("ip tuntap add name ogstun"), "create ogstun"),
                () -> assertTrue(sh.contains("ip tuntap add name ogstun2"), "create ogstun2"),
                () -> assertTrue(sysctlPos > 0, "sysctl exists"),
                () -> assertTrue(linkUpPos > 0, "link up exists"),
                () -> assertTrue(sysctlPos < linkUpPos, "sysctl BEFORE link up"),
                () -> assertTrue(linkUpPos < iptablesPos, "link up BEFORE iptables"),
                () -> assertTrue(iptablesPos < upfdPos, "iptables BEFORE upfd")
        );

        System.out.println("=== wrapper.sh ===");
        System.out.println(sh);
    }

    @Test
    void commonNf_udrHasDbUri() {
        CommonNfYamlRenderer renderer = new CommonNfYamlRenderer();
        String udr = renderer.renderUdr(global);

        assertTrue(udr.contains("db_uri: mongodb://mongodb/open5gs"), "UDR db_uri");
        assertTrue(udr.contains("ue: 1024"), "global.max.ue");
        assertTrue(udr.contains("gnb: 64"), "global.max.gnb");

        System.out.println("=== udrcfg.yaml ===");
        System.out.println(udr);
    }

    @Test
    void commonNf_udmHasHnet() {
        CommonNfYamlRenderer renderer = new CommonNfYamlRenderer();
        String udm = renderer.renderUdm(global);

        assertTrue(udm.contains("hnet:"), "UDM hnet section");
        assertTrue(udm.contains("curve25519-1.key"), "hnet key 1");
        assertTrue(udm.contains("secp256r1-2.key"), "hnet key 2");
        assertTrue(udm.contains("scheme: 1"), "scheme 1");
        assertTrue(udm.contains("scheme: 2"), "scheme 2");

        System.out.println("=== udmcfg.yaml ===");
        System.out.println(udm);
    }

    @Test
    void commonNf_ausfUsesAddress() {
        CommonNfYamlRenderer renderer = new CommonNfYamlRenderer();
        String ausf = renderer.renderAusf(global);

        assertTrue(ausf.contains("address: 0.0.0.0"), "AUSF SBI address");
        assertFalse(ausf.contains("dev: eth0"), "AUSF should NOT use dev: eth0");

        System.out.println("=== ausfcfg.yaml ===");
        System.out.println(ausf);
    }

    @Test
    void nrfYaml_hasTaiAndMax() {
        NrfYamlRenderer renderer = new NrfYamlRenderer();
        String yaml = renderer.render(amf, global);

        assertAll("nrfcfg.yaml checks",
                () -> assertTrue(yaml.contains("ue: 1024"), "global.max.ue"),
                () -> assertTrue(yaml.contains("gnb: 64"), "global.max.gnb"),
                () -> assertTrue(yaml.contains("mcc: 999"), "plmn mcc in serving"),
                () -> assertTrue(yaml.contains("mnc: 70"), "plmn mnc in serving")
        );

        System.out.println("=== nrfcfg.yaml ===");
        System.out.println(yaml);
    }

    @Test
    void nssfYaml_hasSliceAndMax() {
        NssfYamlRenderer renderer = new NssfYamlRenderer();
        String yaml = renderer.render(amf, global);

        assertAll("nssfcfg.yaml checks",
                () -> assertTrue(yaml.contains("ue: 1024"), "global.max.ue"),
                () -> assertTrue(yaml.contains("gnb: 64"), "global.max.gnb"),
                () -> assertTrue(yaml.contains("sst: 1"), "slice sst")
        );

        System.out.println("=== nssfcfg.yaml ===");
        System.out.println(yaml);
    }

    @Test
    void mmeYaml_4gParameters() {
        global.setNetworkMode(GlobalConfig.NetworkMode.HYBRID_4G_5G);

        amf.setMmeName("open5gs-mme0");
        amf.setS1cInterfaceIp("10.10.1.2");

        AmfConfig.MmeId mmeId = new AmfConfig.MmeId();
        mmeId.setMmegi(32768);
        mmeId.setMmec(1);
        amf.setMmeId(mmeId);

        MmeYamlRenderer renderer = new MmeYamlRenderer();
        String yaml = renderer.render(amf, global);

        assertAll("mmecfg.yaml checks",
                () -> assertTrue(yaml.contains("mme_name: open5gs-mme0"), "mme_name"),
                () -> assertTrue(yaml.contains("mme_gid: 32768"), "mme_gid"),
                () -> assertTrue(yaml.contains("mme_code: 1"), "mme_code"),
                () -> assertTrue(yaml.contains("address: 10.10.1.2"), "s1ap address"),
                () -> assertTrue(yaml.contains("full: Opticoms 5G Network"), "network_name.full"),
                () -> assertTrue(yaml.contains("short: OPTC5G"), "network_name.short"),
                () -> assertTrue(yaml.contains("t3402:"), "nas timer t3402"),
                () -> assertTrue(yaml.contains("t3412:"), "nas timer t3412"),
                () -> assertTrue(yaml.contains("t3423:"), "nas timer t3423"),
                () -> assertTrue(yaml.contains("ue: 1024"), "global.max.ue"),
                () -> assertTrue(yaml.contains("gnb: 64"), "global.max.gnb")
        );

        System.out.println("=== mmecfg.yaml ===");
        System.out.println(yaml);
    }
}
