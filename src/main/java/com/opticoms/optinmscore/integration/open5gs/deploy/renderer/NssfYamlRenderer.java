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
 * AmfConfig + GlobalConfig → nssfcfg.yaml string üretir.
 *
 * LLD Parametre → YAML Path eşlemesi (Tablo 3 + Tablo 5):
 *
 *  GlobalConfig.maxSupportedDevices    → global.max.ue
 *  AmfConfig.supportedSlices           → nssf.sbi.client.nsi[*].s_nssai
 *
 *  NSSF'in rolü: UE'nin istediği slice'ı hangi NRF'in hizmet verdiğini söyler.
 *  Her slice için bir NSI (Network Slice Instance) entry üretilir.
 */
@Component
public class NssfYamlRenderer {

    private final Yaml yaml;

    public NssfYamlRenderer() {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setExplicitStart(false);
        this.yaml = new Yaml(opts);
    }

    /**
     * @param amf    AmfConfig    — Slice listesi için
     * @param global GlobalConfig — max.ue için
     * @return nssfcfg.yaml içeriği (String)
     */
    public String render(AmfConfig amf, GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();

        // ── logger ──────────────────────────────────────────────────────────
        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/nssf.log");
        root.put("logger", logger);

        // ── global.max ──────────────────────────────────────────────────────
        // LLD Tablo 3: Max-Nbr-UEs + Max-Nbr-NBs → YAML files: ALL
        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        root.put("global", globalSection);

        // ── nssf ─────────────────────────────────────────────────────────────
        Map<String, Object> nssfSection = new LinkedHashMap<>();

        // sbi: NSSF'in hem server hem client konfigürasyonu
        nssfSection.put("sbi", buildSbiSection(amf));

        root.put("nssf", nssfSection);

        return yaml.dump(root);
    }

    private Map<String, Object> buildSbiSection(AmfConfig amf) {
        Map<String, Object> sbi = new LinkedHashMap<>();

        // Server: K8s service discovery
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", "eth0");
        server.put("advertise", "nssf-nnssf");
        server.put("port", 80);
        servers.add(server);
        sbi.put("server", servers);

        // Client: SCP + NSI (NRF başvurusu, her slice için)
        Map<String, Object> client = new LinkedHashMap<>();

        // SCP referansı
        List<Map<String, Object>> scpList = new ArrayList<>();
        Map<String, Object> scp = new LinkedHashMap<>();
        scp.put("uri", "http://scp-nscp:80");
        scpList.add(scp);
        client.put("scp", scpList);

        // NSI: Her slice için bir entry → hangi NRF'e gidileceğini söyler
        // LLD Tablo 5: Slice-List (SST/SD) → nssf.yaml
        List<Map<String, Object>> nsiList = new ArrayList<>();
        for (AmfConfig.Slice slice : amf.getSupportedSlices()) {
            Map<String, Object> nsiEntry = new LinkedHashMap<>();
            nsiEntry.put("uri", "http://nrf-nnrf:80");

            Map<String, Object> sNssai = new LinkedHashMap<>();
            sNssai.put("sst", slice.getSst());
            sNssai.put("sd", normalizeSd(slice.getSd()));
            nsiEntry.put("s_nssai", sNssai);

            nsiList.add(nsiEntry);
        }
        client.put("nsi", nsiList);

        sbi.put("client", client);
        return sbi;
    }

    private String normalizeSd(String sd) {
        if (sd == null || sd.isBlank() || sd.equalsIgnoreCase("FFFFFF")) {
            return "ffffff";
        }
        return sd.toLowerCase();
    }
}
