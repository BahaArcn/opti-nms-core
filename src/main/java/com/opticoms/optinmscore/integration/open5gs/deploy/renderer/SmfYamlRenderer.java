package com.opticoms.optinmscore.integration.open5gs.deploy.renderer;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SmfConfig + GlobalConfig → smfcfg.yaml string üretir.
 *
 * LLD Parametre → YAML Path eşlemesi (Tablo 3 + Tablo 4 + Tablo 5):
 *
 *  GlobalConfig.maxSupportedDevices         → global.max.ue
 *
 *  SmfConfig.mtu                            → smf.mtu
 *  SmfConfig.dnsIps                         → smf.dns[*]
 *  SmfConfig.securityIndication.integrity   → smf.security_indication.integrity_protection_indication
 *  SmfConfig.securityIndication.ciphering   → smf.security_indication.confidentiality_protection_indication
 *
 *  GlobalConfig.ueIpPoolList[*].ipRange      → smf.session[*].subnet (via tunInterface lookup)
 *  SmfConfig.apnList[*].apnDnnName          → smf.session[*].dnn  (opsiyonel)
 *  SmfConfig.apnList[*].local=false         → smf.pfcp.client.upf[*].address = remoteUpfIp + dnn
 *  SmfConfig.apnList[*].local=true          → smf.pfcp.client.upf[*].address = UpfConfig.n4PfcpIp + dnn
 *
 *  SmfConfig.apnList[*].sliceId (sst/sd)    → smf.info[*].s_nssai[*].sst/sd
 *  SmfConfig.apnList[*].apnDnnName          → smf.info[*].s_nssai[*].dnn[*]
 */
@Component
public class SmfYamlRenderer {

    private final Yaml yaml;

    public SmfYamlRenderer() {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setExplicitStart(false);
        this.yaml = new Yaml(opts);
    }

    /**
     * @param smf    SmfConfig — SMF'e özgü tüm parametreler
     * @param global GlobalConfig — max.ue için
     * @param upf    UpfConfig — PFCP client address (n4PfcpIp) için
     * @return smfcfg.yaml içeriği (String)
     */
    public String render(SmfConfig smf, GlobalConfig global, UpfConfig upf) {
        Map<String, Object> root = new LinkedHashMap<>();

        // ── logger ──────────────────────────────────────────────────────────
        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/smf.log");
        root.put("logger", logger);

        // ── global.max ──────────────────────────────────────────────────────
        // LLD Tablo 3: Max-Nbr-UEs + Max-Nbr-NBs → YAML files: ALL
        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        root.put("global", globalSection);

        // ── smf ─────────────────────────────────────────────────────────────
        Map<String, Object> smfSection = new LinkedHashMap<>();

        // sbi: sabit K8s service discovery
        smfSection.put("sbi", buildSbiSection());

        // pfcp: SMF ↔ UPF kontrolü için PFCP
        // local=true  → UPF'in n4PfcpIp'si + DNN adı
        // local=false → remoteUpfIp + DNN adı (CUPS)
        smfSection.put("pfcp", buildPfcpSection(smf, upf));

        // gtpc/gtpu: 4G/5G data plane — K8s interface bazlı
        smfSection.put("gtpc", buildSimpleDevSection("gtpc", "eth0"));
        smfSection.put("gtpu", buildSimpleDevSection("gtpu", "n3"));

        // metrics: Prometheus
        smfSection.put("metrics", buildMetricsSection());

        // session: UE IP havuzları (from GlobalConfig.ueIpPoolList via tunInterface lookup)
        smfSection.put("session", buildSessionList(smf, global));

        // dns: SMF override > Global fallback
        List<String> effectiveDns = smf.getSmfDnsIps() != null && !smf.getSmfDnsIps().isEmpty()
                ? smf.getSmfDnsIps() : global.getDnsIps();
        if (effectiveDns != null && !effectiveDns.isEmpty()) {
            smfSection.put("dns", effectiveDns);
        }

        // mtu: SMF override > Global fallback
        int effectiveMtu = smf.getSmfMtu() != null ? smf.getSmfMtu() : global.getMtu();
        smfSection.put("mtu", effectiveMtu);

        // ctf: charging — Open5GS default
        Map<String, Object> ctf = new LinkedHashMap<>();
        ctf.put("enabled", "auto");
        smfSection.put("ctf", ctf);

        // freeDiameter: 4G Diameter protokol konfigürasyonu (sabit yol)
        smfSection.put("freeDiameter", "/open5gs/install/etc/freeDiameter/smf.conf");

        // security_indication: 5G user plane güvenlik göstergesi
        // LLD Tablo 5: Sec-ind-5g → smf.yaml
        smfSection.put("security_indication", buildSecurityIndicationSection(smf));

        // info: Slice ↔ DNN ilişkisi
        // LLD Tablo 5: Slice-List + Dnn-List → smf.yaml
        smfSection.put("info", buildInfoSection(smf));

        root.put("smf", smfSection);

        return yaml.dump(root);
    }

    // ── Private builder metodları ────────────────────────────────────────────

    private Map<String, Object> buildSbiSection() {
        Map<String, Object> sbi = new LinkedHashMap<>();

        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", "eth0");
        server.put("advertise", "smf1-nsmf");
        server.put("port", 80);
        servers.add(server);
        sbi.put("server", servers);

        Map<String, Object> client = new LinkedHashMap<>();
        List<Map<String, Object>> scpList = new ArrayList<>();
        Map<String, Object> scp = new LinkedHashMap<>();
        scp.put("uri", "http://scp-nscp:80");
        scpList.add(scp);
        client.put("scp", scpList);
        sbi.put("client", client);

        return sbi;
    }

    private Map<String, Object> buildPfcpSection(SmfConfig smf, UpfConfig upf) {
        Map<String, Object> pfcp = new LinkedHashMap<>();

        // PFCP server: SMF tarafı — K8s n4 interface üzerinden dinle
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", "n4");
        servers.add(server);
        pfcp.put("server", servers);

        // PFCP client: SMF → UPF bağlantısı
        // Her DNN için ayrı bir UPF entry üretilir
        // local=true  → UpfConfig.n4PfcpIp (UPF'in n4 interface adresi) + DNN
        // local=false → SmfConfig.ApnDnn.remoteUpfIp (CUPS remote UPF)
        List<Map<String, Object>> upfList = new ArrayList<>();

        for (SmfConfig.ApnDnn dnn : smf.getApnList()) {
            Map<String, Object> upfEntry = new LinkedHashMap<>();

            if (dnn.isLocal()) {
                // Bug 2 düzeltmesi: "dev: n4" yerine UPF'in gerçek PFCP listen IP'si
                // n4PfcpIp boşsa fallback: n4 dev kullan (K8s auto-discovery için)
                if (upf != null && upf.getN4PfcpIp() != null && !upf.getN4PfcpIp().isBlank()) {
                    upfEntry.put("address", upf.getN4PfcpIp());
                } else {
                    // n4PfcpIp henüz girilmemişse güvenli fallback
                    upfEntry.put("dev", "n4");
                }
            } else {
                // CUPS: remote UPF IP
                if (dnn.getRemoteUpfIp() != null && !dnn.getRemoteUpfIp().isBlank()) {
                    upfEntry.put("address", dnn.getRemoteUpfIp());
                }
            }
            upfEntry.put("dnn", dnn.getApnDnnName());
            upfList.add(upfEntry);
        }

        Map<String, Object> pfcpClient = new LinkedHashMap<>();
        pfcpClient.put("upf", upfList);
        pfcp.put("client", pfcpClient);

        return pfcp;
    }

    private Map<String, Object> buildSimpleDevSection(String sectionName, String devName) {
        Map<String, Object> section = new LinkedHashMap<>();
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", devName);
        servers.add(server);
        section.put("server", servers);
        return section;
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

    private List<Map<String, Object>> buildSessionList(SmfConfig smf, GlobalConfig global) {
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (SmfConfig.ApnDnn dnn : smf.getApnList()) {
            GlobalConfig.UeIpPool pool = findPool(global, dnn.getTunInterface());
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("subnet", buildSubnetFromGatewayAndRange(
                    pool.getGatewayIp(), pool.getIpRange()));
            sessions.add(session);
        }
        return sessions;
    }

    private GlobalConfig.UeIpPool findPool(GlobalConfig global, String tunInterface) {
        if (global.getUeIpPoolList() != null) {
            for (GlobalConfig.UeIpPool pool : global.getUeIpPoolList()) {
                if (pool.getTunInterface().equals(tunInterface)) {
                    return pool;
                }
            }
        }
        throw new IllegalStateException("UeIpPool not found for tunInterface: " + tunInterface);
    }

    private String buildSubnetFromGatewayAndRange(String gatewayIp, String ueIpRange) {
        if (ueIpRange.contains("/")) {
            String prefix = ueIpRange.substring(ueIpRange.indexOf("/"));
            return gatewayIp + prefix;
        }
        // Range format (x.x.x.x-y.y.y.y) varsa subnet belirlenemez, ham değer dön
        return ueIpRange;
    }

    private Map<String, Object> buildSecurityIndicationSection(SmfConfig smf) {
        // LLD Tablo 5: Sec-ind-5g
        // RequirementLevel enum → lowercase string (REQUIRED→required, NOT_NEEDED→not-needed)
        Map<String, Object> secInd = new LinkedHashMap<>();
        secInd.put("integrity_protection_indication",
                requirementLevelToYaml(smf.getSecurityIndication().getIntegrity()));
        secInd.put("confidentiality_protection_indication",
                requirementLevelToYaml(smf.getSecurityIndication().getCiphering()));
        return secInd;
    }

    private String requirementLevelToYaml(SmfConfig.RequirementLevel level) {
        return switch (level) {
            case REQUIRED -> "required";
            case PREFERRED -> "preferred";
            case NOT_NEEDED -> "not-needed";
        };
    }

    private List<Map<String, Object>> buildInfoSection(SmfConfig smf) {
        // smf.info: slice ↔ DNN eşlemesi
        // Her benzersiz slice için bir entry, her DNN o slice'a ekleniyor
        // Bu bölüm AMF'e hangi slices üzerinden hangi DNN'lere servis vereceğini söyler
        List<Map<String, Object>> infoList = new ArrayList<>();

        Map<String, Object> infoEntry = new LinkedHashMap<>();
        List<Map<String, Object>> sNssaiList = new ArrayList<>();

        // Her DNN kendi slice'ına ait bir entry
        for (SmfConfig.ApnDnn dnn : smf.getApnList()) {
            Map<String, Object> nssaiEntry = new LinkedHashMap<>();
            if (dnn.getSliceId() != null) {
                nssaiEntry.put("sst", dnn.getSliceId().getSst());
                nssaiEntry.put("sd", normalizeSd(dnn.getSliceId().getSd()));
            } else {
                // Default: slice 1, SD yok
                nssaiEntry.put("sst", 1);
            }
            nssaiEntry.put("dnn", List.of(dnn.getApnDnnName()));
            sNssaiList.add(nssaiEntry);
        }

        infoEntry.put("s_nssai", sNssaiList);
        infoList.add(infoEntry);
        return infoList;
    }

    private String normalizeSd(String sd) {
        if (sd == null || sd.isBlank() || sd.equalsIgnoreCase("FFFFFF")) {
            return "ffffff";
        }
        return sd.toLowerCase();
    }
}
