package com.opticoms.optinmscore.integration.open5gs.deploy.renderer;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders UpfConfig + SmfConfig + GlobalConfig into upfcfg.yaml and wrapper.sh.
 *
 * LLD parameter → YAML path (tables 3 + 5):
 *
 *  GlobalConfig.maxSupportedDevices         → global.max.ue
 *
 *  UpfConfig.n3InterfaceIp                  → upf.gtpu.server[0].address
 *    (if null, uses dev: n3)
 *
 *  GlobalConfig.ueIpPoolList[*].ipRange     → upf.session[*].subnet (via tunInterface lookup)
 *  SmfConfig.apnList[*].apnDnnName          → upf.session[*].dnn
 *  SmfConfig.apnList[*].tunInterface        → upf.session[*].dev + wrapper.sh tun device name
 *  GlobalConfig.ueIpPoolList[*].gatewayIp   → wrapper.sh: ip addr add <gateway>/prefix dev <tunInterface>
 *  GlobalConfig.ueIpPoolList[*].ipRange     → wrapper.sh: iptables -s <network>
 */
@Component
public class UpfYamlRenderer {

    private final Yaml yaml;

    public UpfYamlRenderer() {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setExplicitStart(false);
        this.yaml = new Yaml(opts);
    }

    /** @return upfcfg.yaml content */
    public String renderYaml(UpfConfig upf, SmfConfig smf, GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/upf.log");
        logger.put("level", "info");
        root.put("logger", logger);

        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        root.put("global", globalSection);

        Map<String, Object> upfSection = new LinkedHashMap<>();
        upfSection.put("pfcp", buildPfcpSection());
        upfSection.put("gtpu", buildGtpuSection(upf.getN3InterfaceIp()));
        upfSection.put("session", buildSessionList(smf, global));
        upfSection.put("metrics", buildMetricsSection());

        root.put("upf", upfSection);

        return yaml.dump(root);
    }

    /**
     * wrapper.sh: init script run when the UPF container starts.
     * Creates a tun device per local DNN using {@code tunInterface}.
     */
    public String renderWrapperScript(SmfConfig smf, GlobalConfig global) {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");

        List<SmfConfig.ApnDnn> localDnns = smf.getApnList().stream()
                .filter(SmfConfig.ApnDnn::isLocal)
                .toList();

        for (SmfConfig.ApnDnn dnn : localDnns) {
            GlobalConfig.UeIpPool pool = findPool(global, dnn.getTunInterface());
            String tunName = dnn.getTunInterface();
            String gatewayWithPrefix = buildGatewayWithPrefix(pool.getGatewayIp(), pool.getIpRange());

            sb.append("# DNN: ").append(dnn.getApnDnnName()).append("\n");
            sb.append("ip tuntap add name ").append(tunName).append(" mode tun;\n");
            sb.append("ip addr add ").append(gatewayWithPrefix).append(" dev ").append(tunName).append(";\n");
        }

        sb.append("\n# System settings\n");
        sb.append("sysctl -w net.ipv6.conf.all.disable_ipv6=1;\n");
        sb.append("sysctl -w net.ipv4.conf.all.rp_filter=0;\n");
        sb.append("sysctl -w net.ipv4.conf.default.rp_filter=0;\n");
        sb.append("sysctl -w net.ipv4.conf.n3.rp_filter=0;\n");
        sb.append("\n");

        for (SmfConfig.ApnDnn dnn : localDnns) {
            sb.append("ip link set ").append(dnn.getTunInterface()).append(" up;\n");
        }

        sb.append("sh -c \"echo 1 > /proc/sys/net/ipv4/ip_forward\";\n");
        sb.append("\n");

        for (SmfConfig.ApnDnn dnn : localDnns) {
            GlobalConfig.UeIpPool pool = findPool(global, dnn.getTunInterface());
            String networkCidr = extractNetworkCidr(pool.getIpRange());

            sb.append("iptables -t nat -A POSTROUTING -s ").append(networkCidr)
                    .append(" ! -o ").append(dnn.getTunInterface()).append(" -j MASQUERADE;\n");
        }
        sb.append("\n");

        sb.append("/open5gs/install/bin/open5gs-upfd -c /open5gs/config/upfcfg.yaml\n");

        return sb.toString();
    }

    private GlobalConfig.UeIpPool findPool(GlobalConfig global, String tunInterface) {
        if (global.getUeIpPoolList() != null) {
            for (GlobalConfig.UeIpPool pool : global.getUeIpPoolList()) {
                if (pool.getTunInterface().equals(tunInterface)) {
                    return pool;
                }
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Configuration error: UeIpPool not found for tunInterface: " + tunInterface);
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

    private Map<String, Object> buildGtpuSection(String n3InterfaceIp) {
        Map<String, Object> gtpu = new LinkedHashMap<>();
        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();

        if (n3InterfaceIp != null && !n3InterfaceIp.isBlank()) {
            server.put("address", n3InterfaceIp);
        } else {
            server.put("dev", "n3");
        }
        servers.add(server);
        gtpu.put("server", servers);
        return gtpu;
    }

    private List<Map<String, Object>> buildSessionList(SmfConfig smf, GlobalConfig global) {
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (SmfConfig.ApnDnn dnn : smf.getApnList()) {
            if (!dnn.isLocal()) continue;

            GlobalConfig.UeIpPool pool = findPool(global, dnn.getTunInterface());

            Map<String, Object> session = new LinkedHashMap<>();
            session.put("subnet", extractNetworkCidr(pool.getIpRange()));
            session.put("dev", dnn.getTunInterface());
            session.put("dnn", dnn.getApnDnnName());
            sessions.add(session);
        }
        return sessions;
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

    private String buildGatewayWithPrefix(String gatewayIp, String ipRange) {
        if (ipRange.contains("/")) {
            String prefix = ipRange.substring(ipRange.indexOf("/"));
            return gatewayIp + prefix;
        }
        return gatewayIp;
    }

    private String extractNetworkCidr(String ipRange) {
        return ipRange;
    }
}
