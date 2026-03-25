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
 * AmfConfig + GlobalConfig → nrfcfg.yaml string üretir.
 *
 * LLD Parametre → YAML Path eşlemesi (Tablo 3 + Tablo 4):
 *
 *  GlobalConfig.maxSupportedDevices    → global.max.ue
 *  AmfConfig.supportedPlmns            → nrf.serving[*].plmn_id
 *
 *  NOT (Gap 1 kısmen): LLD Tablo 4'te Tai-List → nrf.yaml deniyor.
 *  Mevcut Open5GS NRF YAML şablonunda TAI alanı yok.
 *  Şu an sadece PLMN bazlı serving eklenmiştir.
 */
@Component
public class NrfYamlRenderer {

    private final Yaml yaml;

    public NrfYamlRenderer() {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setExplicitStart(false);
        this.yaml = new Yaml(opts);
    }

    /**
     * @param amf    AmfConfig    — PLMN listesi için
     * @param global GlobalConfig — max.ue için
     * @return nrfcfg.yaml içeriği (String)
     */
    public String render(AmfConfig amf, GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();

        // ── logger ──────────────────────────────────────────────────────────
        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/nrf.log");
        root.put("logger", logger);

        // ── global.max ──────────────────────────────────────────────────────
        // LLD Tablo 3: Max-Nbr-UEs + Max-Nbr-NBs → YAML files: ALL
        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        root.put("global", globalSection);

        // ── nrf ─────────────────────────────────────────────────────────────
        Map<String, Object> nrfSection = new LinkedHashMap<>();

        // serving: NRF'in hizmet verdiği PLMN'ler
        // AmfConfig.supportedPlmns → nrf.serving[*].plmn_id
        List<Map<String, Object>> servingList = new ArrayList<>();
        for (AmfConfig.Plmn plmn : amf.getSupportedPlmns()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            Map<String, Object> plmnId = new LinkedHashMap<>();
            plmnId.put("mcc", Integer.parseInt(plmn.getMcc()));
            plmnId.put("mnc", Integer.parseInt(plmn.getMnc()));
            entry.put("plmn_id", plmnId);
            servingList.add(entry);
        }
        nrfSection.put("serving", servingList);

        // sbi: sabit K8s service discovery
        nrfSection.put("sbi", buildSbiSection());

        root.put("nrf", nrfSection);

        return yaml.dump(root);
    }

    private Map<String, Object> buildSbiSection() {
        Map<String, Object> sbi = new LinkedHashMap<>();
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", "eth0");
        server.put("advertise", "nrf-nnrf");
        server.put("port", 80);
        servers.add(server);
        sbi.put("server", servers);
        return sbi;
    }
}
