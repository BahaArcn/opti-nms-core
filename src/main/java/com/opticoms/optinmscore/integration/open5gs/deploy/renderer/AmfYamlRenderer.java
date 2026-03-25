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
 * AmfConfig + GlobalConfig → amfcfg.yaml string üretir.
 *
 * LLD Parametre → YAML Path eşlemesi (Tablo 3 + Tablo 5):
 *
 *  GlobalConfig.maxSupportedDevices  → global.max.ue
 *  GlobalConfig.networkFullName      → amf.network_name.full
 *  GlobalConfig.networkShortName     → amf.network_name.short
 *
 *  AmfConfig.amfName                 → amf.amf_name
 *  AmfConfig.amfId.region/set/pointer → amf.guami[*].amf_id.region/set/pointer
 *  AmfConfig.supportedPlmns          → amf.guami[*].plmn_id  +  amf.plmn_support[*].plmn_id
 *  AmfConfig.supportedTais           → amf.tai[*].plmn_id + tac
 *  AmfConfig.supportedSlices         → amf.plmn_support[*].s_nssai[*]
 *  AmfConfig.securityParameters      → amf.security.integrity_order / ciphering_order
 *  AmfConfig.nasTimers5g.t3512       → amf.time.t3512.value
 *  AmfConfig.n2InterfaceIp           → amf.ngap.server[0].address
 *    (eğer null ise dev: n2 kullanılır — K8s ortamında interface adıyla çalışır)
 */
@Component
public class AmfYamlRenderer {

    private final Yaml yaml;

    public AmfYamlRenderer() {
        DumperOptions opts = new DumperOptions();
        // YAML blok stili: okunabilir, inline değil
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        // İlk satırda "---" çıkmasın
        opts.setExplicitStart(false);
        this.yaml = new Yaml(opts);
    }

    /**
     * @param global GlobalConfig — network_name ve max.ue için
     * @param amf    AmfConfig    — tüm AMF parametreleri için
     * @return amfcfg.yaml içeriği (String)
     */
    public String render(GlobalConfig global, AmfConfig amf) {
        Map<String, Object> root = new LinkedHashMap<>();

        // ── logger ──────────────────────────────────────────────────────────
        // Sabit: container içi log dosyası yolu değişmiyor
        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/amf.log");
        root.put("logger", logger);

        // ── global.max ──────────────────────────────────────────────────────
        // LLD Tablo 3: Max-Nbr-UEs + Max-Nbr-NBs → YAML files: ALL
        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        root.put("global", globalSection);

        // ── amf ─────────────────────────────────────────────────────────────
        Map<String, Object> amfSection = new LinkedHashMap<>();

        // sbi: Service Based Interface — sabit K8s service discovery, değişmiyor
        amfSection.put("sbi", buildSbiSection());

        // ngap: N2 arayüzü (gNB ↔ AMF)
        // LLD Tablo 5: Ngap-Addr → amf.yaml
        // n2InterfaceIp null ise K8s'te interface adı "n2" kullanılır
        amfSection.put("ngap", buildNgapSection(amf.getN2InterfaceIp()));

        // metrics: Prometheus scrape endpoint — sabit
        amfSection.put("metrics", buildMetricsSection());

        // guami: AMF'in kimliği (PLMN + AMF-ID)
        // LLD Tablo 5: Amf-Id (region/set/pointer) → amf.yaml
        amfSection.put("guami", buildGuamiList(amf));

        // tai: Tracking Area Identity listesi
        // LLD Tablo 4 (common 4G&5G): Tai-List → 5G: amf.yaml, nrf.yaml, pcf.yaml, scp.yaml
        amfSection.put("tai", buildTaiList(amf));

        // plmn_support: Desteklenen PLMN'ler ve slice'lar
        // LLD Tablo 5: Slice-List → amf.yaml, nssf.yaml, smf.yaml
        amfSection.put("plmn_support", buildPlmnSupportList(amf));

        // security: 5G bütünlük ve şifreleme algoritmaları sırası
        // LLD Tablo 5: Sec-Order-5g → amf.yaml
        amfSection.put("security", buildSecuritySection(amf));

        // network_name: UE status bar'ında görünen ağ adı
        // LLD Tablo 3: Network-Name (Full/Short) → amf.yaml (5G)
        // Gap 4 çözümü: GlobalConfig'ten geliyor
        amfSection.put("network_name", buildNetworkNameSection(global));

        // amf_name: AMF'in 5GC içindeki ismi
        // LLD Tablo 5: Amf-Name → amf.yaml
        amfSection.put("amf_name", amf.getAmfName());

        // time: NAS timer konfigürasyonu
        // LLD Tablo 5: Nas-Timers-5g (T3502, T3512) → amf.yaml
        amfSection.put("time", buildTimeSection(amf));

        root.put("amf", amfSection);

        return yaml.dump(root);
    }

    // ── Private builder metodları ────────────────────────────────────────────

    private Map<String, Object> buildSbiSection() {
        // SBI adresi K8s service adından geliyor — tenant bağımsız, sabit kalıyor
        Map<String, Object> sbi = new LinkedHashMap<>();
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", "eth0");
        server.put("advertise", "amf-namf");
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

    private Map<String, Object> buildNgapSection(String n2InterfaceIp) {
        Map<String, Object> ngap = new LinkedHashMap<>();
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();

        // Eğer IP adresi verilmişse address kullan, yoksa K8s interface adı
        if (n2InterfaceIp != null && !n2InterfaceIp.isBlank()) {
            server.put("address", n2InterfaceIp);
        } else {
            server.put("dev", "n2");
        }
        servers.add(server);
        ngap.put("server", servers);
        return ngap;
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

    private List<Map<String, Object>> buildGuamiList(AmfConfig amf) {
        // GUAMI = PLMN-ID + AMF-ID (region + set)
        // Her desteklenen PLMN için bir GUAMI entry üretiyoruz
        List<Map<String, Object>> guamiList = new ArrayList<>();
        for (AmfConfig.Plmn plmn : amf.getSupportedPlmns()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("plmn_id", plmnToMap(plmn));

            Map<String, Object> amfId = new LinkedHashMap<>();
            amfId.put("region", amf.getAmfId().getRegion());
            amfId.put("set", amf.getAmfId().getSet());
            amfId.put("pointer", amf.getAmfId().getPointer());
            entry.put("amf_id", amfId);

            guamiList.add(entry);
        }
        return guamiList;
    }

    private List<Map<String, Object>> buildTaiList(AmfConfig amf) {
        // TAI: her TAI entry → plmn_id + tac
        List<Map<String, Object>> taiList = new ArrayList<>();
        for (AmfConfig.Tai tai : amf.getSupportedTais()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("plmn_id", plmnToMap(tai.getPlmn()));
            entry.put("tac", tai.getTac());
            taiList.add(entry);
        }
        return taiList;
    }

    private List<Map<String, Object>> buildPlmnSupportList(AmfConfig amf) {
        // plmn_support: her PLMN için desteklenen slice'lar (s_nssai)
        List<Map<String, Object>> plmnSupportList = new ArrayList<>();
        for (AmfConfig.Plmn plmn : amf.getSupportedPlmns()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("plmn_id", plmnToMap(plmn));

            List<Map<String, Object>> sNssaiList = new ArrayList<>();
            for (AmfConfig.Slice slice : amf.getSupportedSlices()) {
                Map<String, Object> nssai = new LinkedHashMap<>();
                nssai.put("sst", slice.getSst());
                // SD null veya FFFFFF ise "ffffff" yaz (Open5GS convention)
                nssai.put("sd", normalizeSd(slice.getSd()));
                sNssaiList.add(nssai);
            }
            entry.put("s_nssai", sNssaiList);
            plmnSupportList.add(entry);
        }
        return plmnSupportList;
    }

    private Map<String, Object> buildSecuritySection(AmfConfig amf) {
        Map<String, Object> security = new LinkedHashMap<>();
        // LLD: integrity_order = [NIA2, NIA1, NIA0], ciphering_order = [NEA0, NEA1, NEA2]
        security.put("integrity_order", amf.getSecurityParameters().getIntegrityOrder5g());
        security.put("ciphering_order", amf.getSecurityParameters().getCipheringOrder5g());
        return security;
    }

    private Map<String, Object> buildNetworkNameSection(GlobalConfig global) {
        // Gap 4 çözümü: GlobalConfig.networkFullName/networkShortName → amf.network_name
        Map<String, Object> networkName = new LinkedHashMap<>();
        networkName.put("full", global.getNetworkFullName());
        if (global.getNetworkShortName() != null && !global.getNetworkShortName().isBlank()) {
            networkName.put("short", global.getNetworkShortName());
        }
        return networkName;
    }

    private Map<String, Object> buildTimeSection(AmfConfig amf) {
        // LLD Tablo 5: T3502 ve T3512 → amf.time
        // t3502 → deregistration retransmission timer
        // t3512 → periodic registration timer
        Map<String, Object> time = new LinkedHashMap<>();

        Map<String, Object> t3502 = new LinkedHashMap<>();
        t3502.put("value", amf.getNasTimers5g().getT3502());
        time.put("t3502", t3502);

        Map<String, Object> t3512 = new LinkedHashMap<>();
        t3512.put("value", amf.getNasTimers5g().getT3512());
        time.put("t3512", t3512);

        return time;
    }

    // ── Yardımcı metodlar ────────────────────────────────────────────────────

    private Map<String, Object> plmnToMap(AmfConfig.Plmn plmn) {
        Map<String, Object> map = new LinkedHashMap<>();
        // Open5GS YAML'da mcc/mnc integer — string değil
        map.put("mcc", Integer.parseInt(plmn.getMcc()));
        map.put("mnc", Integer.parseInt(plmn.getMnc()));
        return map;
    }

    /**
     * SD değerini normalleştirir.
     * "FFFFFF" veya null → "ffffff" (Open5GS küçük harf kullanıyor)
     */
    private String normalizeSd(String sd) {
        if (sd == null || sd.isBlank() || sd.equalsIgnoreCase("FFFFFF")) {
            return "ffffff";
        }
        return sd.toLowerCase();
    }
}
