package com.opticoms.optinmscore.integration.open5gs.deploy.renderer;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UpfConfig + GlobalConfig → sgwucfg.yaml
 *
 * SGW-U (Serving Gateway - User Plane) is the 4G equivalent of UPF's user-plane.
 * Active only when networkMode is ONLY_4G or HYBRID_4G_5G.
 *
 * LLD Parametre → YAML Path:
 *   GlobalConfig.maxSupportedDevices  → global.max.ue
 *   GlobalConfig.maxSupportedGNBs     → global.max.gnb
 *   UpfConfig.s1uInterfaceIp          → sgwu.gtpu.server[0].address
 */
@Component
public class SgwuYamlRenderer {

    private final Yaml yaml;

    public SgwuYamlRenderer() {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setExplicitStart(false);
        this.yaml = new Yaml(opts);
    }

    public String render(UpfConfig upf, GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();

        // logger
        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/sgwu.log");
        logger.put("level", "info");
        root.put("logger", logger);

        // global.max
        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        root.put("global", globalSection);

        // sgwu
        Map<String, Object> sgwuSection = new LinkedHashMap<>();
        sgwuSection.put("pfcp", buildPfcpSection());
        sgwuSection.put("gtpu", buildGtpuSection(upf.getS1uInterfaceIp()));
        root.put("sgwu", sgwuSection);

        return yaml.dump(root);
    }

    private Map<String, Object> buildPfcpSection() {
        Map<String, Object> pfcp = new LinkedHashMap<>();
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", "n4");
        servers.add(server);
        pfcp.put("server", servers);
        return pfcp;
    }

    private Map<String, Object> buildGtpuSection(String s1uInterfaceIp) {
        Map<String, Object> gtpu = new LinkedHashMap<>();
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();

        if (s1uInterfaceIp != null && !s1uInterfaceIp.isBlank()) {
            server.put("address", s1uInterfaceIp);
        } else {
            server.put("dev", "s1u");
        }
        servers.add(server);
        gtpu.put("server", servers);
        return gtpu;
    }
}
