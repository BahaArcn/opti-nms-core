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
 * Renders AmfConfig + GlobalConfig into nssfcfg.yaml.
 *
 * LLD parameter → YAML path (tables 3 + 5):
 *
 *  GlobalConfig.maxSupportedDevices    → global.max.ue
 *  AmfConfig.supportedSlices           → nssf.sbi.client.nsi[*].s_nssai
 *
 *  NSSF maps each requested slice to the NRF that serves it.
 *  One NSI (Network Slice Instance) entry is emitted per slice.
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
     * @param amf    slice list
     * @param global global limits (e.g. max.ue)
     */
    public String render(AmfConfig amf, GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();

        // ── logger ──────────────────────────────────────────────────────────
        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/nssf.log");
        root.put("logger", logger);

        // ── global.max ──────────────────────────────────────────────────────
        // LLD Table 3: Max-Nbr-UEs + Max-Nbr-NBs → YAML files: ALL
        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        root.put("global", globalSection);

        // ── nssf ─────────────────────────────────────────────────────────────
        Map<String, Object> nssfSection = new LinkedHashMap<>();

        // sbi: NSSF server + client
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

        // Client: SCP + NSI (NRF URI per slice)
        Map<String, Object> client = new LinkedHashMap<>();

        // SCP
        List<Map<String, Object>> scpList = new ArrayList<>();
        Map<String, Object> scp = new LinkedHashMap<>();
        scp.put("uri", "http://scp-nscp:80");
        scpList.add(scp);
        client.put("scp", scpList);

        // NSI: one entry per slice → target NRF
        // LLD Table 5: Slice-List (SST/SD) → nssf.yaml
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
