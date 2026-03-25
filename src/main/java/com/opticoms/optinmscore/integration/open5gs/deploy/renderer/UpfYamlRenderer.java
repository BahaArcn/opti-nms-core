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
 * UpfConfig + SmfConfig + GlobalConfig → upfcfg.yaml + wrapper.sh üretir.
 *
 * Gap 3 çözümü: UPF YAML'ı SmfConfig.apnList'e bağımlı.
 * Gap 5 çözümü: wrapper.sh dinamik olarak apnList'ten üretiliyor.
 *
 * LLD Parametre → YAML Path eşlemesi (Tablo 3 + Tablo 5):
 *
 *  GlobalConfig.maxSupportedDevices         → global.max.ue
 *
 *  UpfConfig.n3InterfaceIp                  → upf.gtpu.server[0].address
 *    (null ise dev: n3 kullanılır)
 *
 *  SmfConfig.apnList[*].ueIpRange           → upf.session[*].subnet (network CIDR)
 *  SmfConfig.apnList[*].apnDnnName          → upf.session[*].dnn
 *  SmfConfig.apnList[*].gatewayIp           → wrapper.sh: ip addr add <gateway>/prefix dev ogstun
 *  SmfConfig.apnList[*].ueIpRange (network) → wrapper.sh: iptables -s <network>
 *
 *  wrapper.sh İçeriği:
 *    - Her DNN için: tun interface yarat, IP ata, link up
 *    - ip_forward, rp_filter ayarları
 *    - Her DNN için: iptables MASQUERADE kuralı
 *    - open5gs-upfd başlatma
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

    /**
     * @return upfcfg.yaml içeriği (String)
     */
    public String renderYaml(UpfConfig upf, SmfConfig smf, GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();

        // ── logger ──────────────────────────────────────────────────────────
        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/upf.log");
        logger.put("level", "info");
        root.put("logger", logger);

        // ── global.max ──────────────────────────────────────────────────────
        // LLD Tablo 3: Max-Nbr-UEs + Max-Nbr-NBs → YAML files: ALL
        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        root.put("global", globalSection);

        // ── upf ─────────────────────────────────────────────────────────────
        Map<String, Object> upfSection = new LinkedHashMap<>();

        // pfcp: N4 arayüzü (SMF ↔ UPF control plane) — K8s n4 interface
        upfSection.put("pfcp", buildPfcpSection());

        // gtpu: N3 arayüzü (gNB ↔ UPF user plane)
        // LLD Tablo 5: N3-Addr → upf.yaml
        upfSection.put("gtpu", buildGtpuSection(upf.getN3InterfaceIp()));

        // session: UE IP havuzları
        // Gap 3: SmfConfig.apnList'ten geliyor
        // LLD Tablo 5: Dnn-List → smf.yaml and upf.yaml
        upfSection.put("session", buildSessionList(smf));

        // metrics: Prometheus
        upfSection.put("metrics", buildMetricsSection());

        root.put("upf", upfSection);

        return yaml.dump(root);
    }

    /**
     * wrapper.sh: UPF container başladığında çalışan init script.
     * Her DNN için ayrı ogstun interface yarat.
     *
     * Gap 5 çözümü: apnList değişince wrapper.sh de yeniden üretilir.
     *
     * @return wrapper.sh içeriği (String)
     */
    public String renderWrapperScript(SmfConfig smf) {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash\n\n");

        List<SmfConfig.ApnDnn> localDnns = smf.getApnList().stream()
                .filter(SmfConfig.ApnDnn::isLocal)
                .toList();

        // Her local DNN için tun interface oluştur
        // Birden fazla DNN varsa ogstun, ogstun2, ogstun3... adlandırması
        for (int i = 0; i < localDnns.size(); i++) {
            SmfConfig.ApnDnn dnn = localDnns.get(i);
            String tunName = i == 0 ? "ogstun" : "ogstun" + (i + 1);
            String gatewayWithPrefix = buildGatewayWithPrefix(dnn.getGatewayIp(), dnn.getUeIpRange());
            String networkCidr = extractNetworkCidr(dnn.getUeIpRange());

            // Orijinal wrapper.sh sırası: create → assign IP → sysctl → link up → ip_forward → iptables
            // sysctl rp_filter=0 MUTLAKA ip link set up'tan önce gelmeli;
            // aksi hâlde kernel interface geldiğinde default rp_filter ile başlar ve GTP drop olur.
            sb.append("# DNN: ").append(dnn.getApnDnnName()).append("\n");
            sb.append("ip tuntap add name ").append(tunName).append(" mode tun;\n");
            sb.append("ip addr add ").append(gatewayWithPrefix).append(" dev ").append(tunName).append(";\n");
        }

        // Sistem geneli ayarlar — ip link set up'tan ÖNCE
        sb.append("\n# Sistem ayarları\n");
        sb.append("sysctl -w net.ipv6.conf.all.disable_ipv6=1;\n");
        sb.append("sysctl -w net.ipv4.conf.all.rp_filter=0;\n");
        sb.append("sysctl -w net.ipv4.conf.default.rp_filter=0;\n");
        sb.append("sysctl -w net.ipv4.conf.n3.rp_filter=0;\n");
        sb.append("\n");

        // Interface'leri up yap ve iptables kural ekle — sysctl'den SONRA
        for (int i = 0; i < localDnns.size(); i++) {
            SmfConfig.ApnDnn dnn = localDnns.get(i);
            String tunName = i == 0 ? "ogstun" : "ogstun" + (i + 1);
            String networkCidr = extractNetworkCidr(dnn.getUeIpRange());

            sb.append("ip link set ").append(tunName).append(" up;\n");
        }

        sb.append("sh -c \"echo 1 > /proc/sys/net/ipv4/ip_forward\";\n");
        sb.append("\n");

        // iptables MASQUERADE kuralları
        for (int i = 0; i < localDnns.size(); i++) {
            SmfConfig.ApnDnn dnn = localDnns.get(i);
            String tunName = i == 0 ? "ogstun" : "ogstun" + (i + 1);
            String networkCidr = extractNetworkCidr(dnn.getUeIpRange());

            sb.append("iptables -t nat -A POSTROUTING -s ").append(networkCidr)
                    .append(" ! -o ").append(tunName).append(" -j MASQUERADE;\n");
        }
        sb.append("\n");

        // UPF process'i başlat
        sb.append("/open5gs/install/bin/open5gs-upfd -c /open5gs/config/upfcfg.yaml\n");

        return sb.toString();
    }

    // ── Private builder metodları ────────────────────────────────────────────

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

    private List<Map<String, Object>> buildSessionList(SmfConfig smf) {
        // upf.session: UPF'in yönettiği UE IP havuzları
        // Sadece local=true olan DNN'ler (remote UPF'e giden DNN'ler buraya girmez)
        //
        // GAP-03 düzeltmesi: Birden fazla DNN varsa tun interface adları
        // wrapper.sh ile uyumlu olmalı: ogstun, ogstun2, ogstun3...
        List<Map<String, Object>> sessions = new ArrayList<>();
        int index = 0;
        for (SmfConfig.ApnDnn dnn : smf.getApnList()) {
            if (!dnn.isLocal()) continue;

            String devName = (index == 0) ? "ogstun" : "ogstun" + (index + 1);

            Map<String, Object> session = new LinkedHashMap<>();
            // upf.session.subnet = network CIDR (10.41.0.0/16 formatı, gateway değil)
            session.put("subnet", extractNetworkCidr(dnn.getUeIpRange()));
            session.put("dev", devName);
            session.put("dnn", dnn.getApnDnnName());
            sessions.add(session);
            index++;
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

    /**
     * gatewayIp=10.45.0.1, ueIpRange=10.45.0.0/16 → "10.45.0.1/16"
     * wrapper.sh'deki "ip addr add" komutunda kullanılır.
     */
    private String buildGatewayWithPrefix(String gatewayIp, String ueIpRange) {
        if (ueIpRange.contains("/")) {
            String prefix = ueIpRange.substring(ueIpRange.indexOf("/"));
            return gatewayIp + prefix;
        }
        return gatewayIp;
    }

    /**
     * ueIpRange=10.45.0.0/16 → "10.45.0.0/16" (network CIDR, gateway değil)
     * upf.session.subnet ve iptables -s parametresinde kullanılır.
     */
    private String extractNetworkCidr(String ueIpRange) {
        // ueIpRange zaten CIDR formatında (10.45.0.0/16), direkt dön
        // Range formatı (10.45.0.2-10.45.0.100) UPF'de desteklenmez, CIDR gerekir
        return ueIpRange;
    }
}
