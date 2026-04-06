package com.opticoms.optinmscore.integration.open5gs.deploy.renderer;

import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders AmfConfig + GlobalConfig into mmecfg.yaml (4G and HYBRID modes).
 *
 * GAP-01: LLD table 6 — 4G core parameters.
 *
 * LLD parameter → YAML path (tables 3 + 4 + 6):
 *
 *  GlobalConfig.maxSupportedDevices        → global.max.ue
 *  GlobalConfig.maxSupportedGNBs           → global.max.gnb
 *  GlobalConfig.networkFullName            → mme.network_name.full
 *  GlobalConfig.networkShortName           → mme.network_name.short
 *
 *  AmfConfig.mmeName                       → mme.mme_name
 *  AmfConfig.mmeId.mmegi / mmec            → mme.gummei[*].mme_gid / mme_code
 *  AmfConfig.supportedPlmns                → mme.gummei[*].plmn_id
 *  AmfConfig.s1cInterfaceIp                → mme.s1ap.server[0].address
 *    (if null, uses dev: s1c)
 *  AmfConfig.supportedTais                 → mme.tai[*].plmn_id + tac
 *  AmfConfig.securityParameters            → mme.security.integrity_order / ciphering_order (4G)
 *  AmfConfig.nasTimers4g.t3402/t3412/t3423 → mme.time.t3402/t3412/t3423.value
 *
 *  GTP-C client: SGWC and SMF (PGW-C) via K8s service DNS
 */
@Component
public class MmeYamlRenderer {

    private final Yaml yaml;

    public MmeYamlRenderer() {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setExplicitStart(false);
        this.yaml = new Yaml(opts);
    }

    /**
     * @param amf    4G MME-related fields (same document as AMF in this model)
     * @param global network display names and global limits
     */
    public String render(AmfConfig amf, GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();

        // ── logger ──────────────────────────────────────────────────────────
        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/mme.log");
        root.put("logger", logger);

        // ── global.max ──────────────────────────────────────────────────────
        // LLD Tablo 3: Max-Nbr-UEs + Max-Nbr-NBs → YAML files: ALL
        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        root.put("global", globalSection);

        // ── mme ─────────────────────────────────────────────────────────────
        Map<String, Object> mmeSection = new LinkedHashMap<>();

        // freeDiameter: 4G Diameter config (fixed path)
        mmeSection.put("freeDiameter", "/open5gs/install/etc/freeDiameter/mme.conf");

        // s1ap: eNB ↔ MME control plane
        // LLD Tablo 6: S1ap-Addr → mme.yaml
        mmeSection.put("s1ap", buildS1apSection(amf.getS1cInterfaceIp()));

        // gtpc: 4G GTP-C toward SGWC and SMF (PGW-C)
        mmeSection.put("gtpc", buildGtpcSection());

        // metrics: Prometheus scrape endpoint
        mmeSection.put("metrics", buildMetricsSection());

        // gummei: Global PLMN > AMF fallback + MME-ID
        mmeSection.put("gummei", buildGummeiList(global, amf));

        // tai: Global TAI > AMF fallback
        mmeSection.put("tai", buildTaiList(global, amf));

        // security: 4G integrity and ciphering (EIA/EEA)
        // LLD Tablo 6: Sec-Order-4g → mme.yaml
        mmeSection.put("security", buildSecuritySection(amf));

        // network_name: display name on the 4G UE
        // LLD Tablo 3: Network-Name → mme.yaml (4G)
        mmeSection.put("network_name", buildNetworkNameSection(global));

        // mme_name: MME name inside the 4G core
        // LLD Tablo 6: Mme-Name → mme.yaml
        mmeSection.put("mme_name", amf.getMmeName() != null ? amf.getMmeName() : "Opti4GC-mme0");

        // time: NAS timers (4G)
        // LLD Tablo 6: Nas-Timers-4g (T3402, T3412, T3423) → mme.yaml
        mmeSection.put("time", buildTimeSection(amf));

        root.put("mme", mmeSection);

        return yaml.dump(root);
    }

    private Map<String, Object> buildS1apSection(String s1cIp) {
        Map<String, Object> s1ap = new LinkedHashMap<>();
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();

        if (s1cIp != null && !s1cIp.isBlank()) {
            server.put("address", s1cIp);
        } else {
            server.put("dev", "s1c");
        }
        servers.add(server);
        s1ap.put("server", servers);
        return s1ap;
    }

    private Map<String, Object> buildGtpcSection() {
        // GTP-C server: MME side
        // GTP-C client: SGWC and SMF (PGW-C) — K8s service DNS
        Map<String, Object> gtpc = new LinkedHashMap<>();

        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", "eth0");
        servers.add(server);
        gtpc.put("server", servers);

        Map<String, Object> client = new LinkedHashMap<>();

        List<Map<String, Object>> sgwcList = new ArrayList<>();
        Map<String, Object> sgwc = new LinkedHashMap<>();
        sgwc.put("address", "sgwc-nsgwc");
        sgwcList.add(sgwc);
        client.put("sgwc", sgwcList);

        // SMF acts as PGW-C (CUPS combined SGWC/PGW-C)
        List<Map<String, Object>> smfList = new ArrayList<>();
        Map<String, Object> smf = new LinkedHashMap<>();
        smf.put("address", "smf1-nsmf");
        smfList.add(smf);
        client.put("smf", smfList);

        gtpc.put("client", client);
        return gtpc;
    }

    private Map<String, Object> buildMetricsSection() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("address", "0.0.0.0");
        server.put("port", 9090);
        servers.add(server);
        metrics.put("server", servers);
        return metrics;
    }

    private List<Map<String, Object>> buildGummeiList(GlobalConfig global, AmfConfig amf) {
        List<Map<String, Object>> gummeiList = new ArrayList<>();
        for (AmfConfig.Plmn plmn : resolveAmfPlmns(global, amf)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("plmn_id", plmnToMap(plmn));

            if (amf.getMmeId() != null) {
                entry.put("mme_gid", amf.getMmeId().getMmegi());
                entry.put("mme_code", amf.getMmeId().getMmec());
            } else {
                entry.put("mme_gid", 2);
                entry.put("mme_code", 1);
            }
            gummeiList.add(entry);
        }
        return gummeiList;
    }

    private List<Map<String, Object>> buildTaiList(GlobalConfig global, AmfConfig amf) {
        List<Map<String, Object>> taiList = new ArrayList<>();

        if (amf.getSupportedTais() != null && !amf.getSupportedTais().isEmpty()) {
            for (AmfConfig.Tai tai : amf.getSupportedTais()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("plmn_id", plmnToMap(tai.getPlmn()));
                entry.put("tac", tai.getTac());
                taiList.add(entry);
            }
        } else if (global.getTaiList() != null) {
            for (GlobalConfig.Tai tai : global.getTaiList()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("plmn_id", globalPlmnToMap(tai.getPlmn()));
                entry.put("tac", tai.getTac());
                taiList.add(entry);
            }
        }
        return taiList;
    }

    private List<AmfConfig.Plmn> resolveAmfPlmns(GlobalConfig global, AmfConfig amf) {
        if (amf.getSupportedPlmns() != null && !amf.getSupportedPlmns().isEmpty()) {
            return amf.getSupportedPlmns();
        }
        List<AmfConfig.Plmn> derived = new ArrayList<>();
        if (global.getTaiList() != null) {
            for (GlobalConfig.Tai tai : global.getTaiList()) {
                AmfConfig.Plmn p = new AmfConfig.Plmn();
                p.setMcc(tai.getPlmn().getMcc());
                p.setMnc(tai.getPlmn().getMnc());
                if (derived.stream().noneMatch(d ->
                        d.getMcc().equals(p.getMcc()) && d.getMnc().equals(p.getMnc()))) {
                    derived.add(p);
                }
            }
        }
        return derived;
    }

    private Map<String, Object> globalPlmnToMap(GlobalConfig.Plmn plmn) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mcc", Integer.parseInt(plmn.getMcc()));
        map.put("mnc", Integer.parseInt(plmn.getMnc()));
        return map;
    }

    private Map<String, Object> buildSecuritySection(AmfConfig amf) {
        Map<String, Object> security = new LinkedHashMap<>();
        // LLD table 6: EIA/EEA algorithm order
        security.put("integrity_order", amf.getSecurityParameters().getIntegrityOrder4g());
        security.put("ciphering_order", amf.getSecurityParameters().getCipheringOrder4g());
        return security;
    }

    private Map<String, Object> buildNetworkNameSection(GlobalConfig global) {
        Map<String, Object> networkName = new LinkedHashMap<>();
        networkName.put("full", global.getNetworkFullName());
        if (global.getNetworkShortName() != null && !global.getNetworkShortName().isBlank()) {
            networkName.put("short", global.getNetworkShortName());
        }
        return networkName;
    }

    private Map<String, Object> buildTimeSection(AmfConfig amf) {
        // LLD Tablo 6: T3402=720, T3412=3240, T3423=720 (default)
        Map<String, Object> time = new LinkedHashMap<>();

        Map<String, Object> t3402 = new LinkedHashMap<>();
        t3402.put("value", amf.getNasTimers4g().getT3402());
        time.put("t3402", t3402);

        Map<String, Object> t3412 = new LinkedHashMap<>();
        t3412.put("value", amf.getNasTimers4g().getT3412());
        time.put("t3412", t3412);

        Map<String, Object> t3423 = new LinkedHashMap<>();
        t3423.put("value", amf.getNasTimers4g().getT3423());
        time.put("t3423", t3423);

        return time;
    }

    private Map<String, Object> plmnToMap(AmfConfig.Plmn plmn) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mcc", Integer.parseInt(plmn.getMcc()));
        map.put("mnc", Integer.parseInt(plmn.getMnc()));
        return map;
    }
}
