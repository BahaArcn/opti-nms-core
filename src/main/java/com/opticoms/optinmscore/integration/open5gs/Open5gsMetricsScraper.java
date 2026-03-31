package com.opticoms.optinmscore.integration.open5gs;

import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.service.PmService;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.repository.TenantRepository;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes metrics from Open5GS UPF via two channels:
 * 1. Prometheus endpoint — session count, QoS flows, memory, CPU
 * 2. N3 interface kernel stats (kubectl exec) — rx/tx bytes and packets
 *
 * Traffic byte/packet counters are intentionally disabled in Open5GS Prometheus
 * (Issue #2210) to avoid data plane performance impact. We read them from
 * /sys/class/net/n3/statistics/ inside the UPF pod instead.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Open5gsMetricsScraper {

    private final TenantRepository tenantRepository;
    private final PmService pmService;
    private final RestTemplate restTemplate;
    private final KubernetesClient k8sClient;

    @Value("${kubernetes.namespace:open5gs}")
    private String k8sNamespace;

    @Value("${open5gs.upf.pod-label:nf=upf}")
    private String upfPodLabel;

    @Value("${open5gs.upf.tun-interface:ogstun}")
    private String tunInterface;

    private static final Pattern METRIC_LINE_PATTERN =
            Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)(?:\\{([^}]*)})? (.+)$");
    private static final Pattern LABEL_PATTERN =
            Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)=\"([^\"]*)\"");

    private static final List<String> TARGET_METRICS = List.of(
            // 3GPP-compliant names (Open5GS v2.7+)
            "fivegs_ep_n3_gtp_indatavolumeqosleveln3upf",
            "fivegs_ep_n3_gtp_outdatavolumeqosleveln3upf",
            "fivegs_upffunction_upf_sessionnbr",
            "fivegs_ep_n3_gtp_indatapktn3upf",
            "fivegs_ep_n3_gtp_outdatapktn3upf",
            "fivegs_upffunction_upf_qosflows",
            "process_resident_memory_bytes",
            "process_cpu_seconds_total",
            // Legacy names (older Open5GS builds)
            "upf_session_rx_bytes",
            "upf_session_tx_bytes",
            "upf_session_count"
    );

    @Scheduled(fixedRateString = "${open5gs.metrics.scrape-interval-ms:30000}")
    @SchedulerLock(name = "upf_metrics_scrape", lockAtMostFor = "28s", lockAtLeastFor = "10s")
    public void scrapeUpfMetrics() {
        List<Tenant> tenants = tenantRepository.findByActiveTrue();
        for (Tenant tenant : tenants) {
            String tenantId = tenant.getTenantId();

            if (tenant.getUpfMetricsUrl() != null && !tenant.getUpfMetricsUrl().isBlank()) {
                try {
                    scrapeForTenant(tenant);
                } catch (Exception e) {
                    log.warn("Failed to scrape UPF Prometheus metrics for tenant {}: {}", tenantId, e.getMessage());
                }
            }

            try {
                scrapeN3InterfaceStats(tenantId);
            } catch (Exception e) {
                log.debug("N3 interface stats not available for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }

    private void scrapeForTenant(Tenant tenant) {
        String body;
        try {
            body = restTemplate.getForObject(tenant.getUpfMetricsUrl(), String.class);
        } catch (Exception e) {
            log.warn("Cannot reach UPF metrics endpoint for tenant {}: {}", tenant.getTenantId(), e.getMessage());
            return;
        }

        if (body == null || body.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();
        String[] lines = body.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            parseAndStore(trimmed, tenant.getTenantId(), now);
        }
    }

    private void parseAndStore(String line, String tenantId, long timestamp) {
        Matcher m = METRIC_LINE_PATTERN.matcher(line);
        if (!m.matches()) {
            return;
        }

        String metricName = m.group(1);
        if (!TARGET_METRICS.contains(metricName)) {
            return;
        }

        String labelsStr = m.group(2);
        String valueStr = m.group(3).trim();

        double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return;
        }

        Map<String, String> labels = parseLabels(labelsStr);

        PmMetric metric = new PmMetric();
        metric.setMetricName(mapMetricName(metricName));
        metric.setValue(value);
        metric.setTimestamp(timestamp);
        metric.setLabels(labels.isEmpty() ? null : labels);
        metric.setMetricType(resolveMetricType(metricName));

        pmService.ingestMetric(tenantId, metric);
    }

    private String mapMetricName(String prometheusName) {
        return switch (prometheusName) {
            case "fivegs_ep_n3_gtp_indatavolumeqosleveln3upf", "upf_session_rx_bytes" -> "upf_rx_bytes";
            case "fivegs_ep_n3_gtp_outdatavolumeqosleveln3upf", "upf_session_tx_bytes" -> "upf_tx_bytes";
            case "fivegs_upffunction_upf_sessionnbr", "upf_session_count" -> "upf_session_count";
            case "fivegs_ep_n3_gtp_indatapktn3upf" -> "upf_rx_packets";
            case "fivegs_ep_n3_gtp_outdatapktn3upf" -> "upf_tx_packets";
            case "fivegs_upffunction_upf_qosflows" -> "upf_qos_flows";
            case "process_resident_memory_bytes" -> "upf_memory_bytes";
            case "process_cpu_seconds_total" -> "upf_cpu_seconds";
            default -> prometheusName;
        };
    }

    private PmMetric.MetricType resolveMetricType(String prometheusName) {
        return switch (prometheusName) {
            case "fivegs_upffunction_upf_sessionnbr",
                 "fivegs_upffunction_upf_qosflows",
                 "process_resident_memory_bytes",
                 "process_open_fds",
                 "pfcp_peers_active" -> PmMetric.MetricType.GAUGE;
            default -> PmMetric.MetricType.COUNTER;
        };
    }

    private Map<String, String> parseLabels(String labelsStr) {
        Map<String, String> labels = new HashMap<>();
        if (labelsStr == null || labelsStr.isBlank()) {
            return labels;
        }
        Matcher lm = LABEL_PATTERN.matcher(labelsStr);
        while (lm.find()) {
            labels.put(lm.group(1), lm.group(2));
        }
        return labels;
    }

    // ── UPF interface kernel statistics ─────────────────────────────────────

    private static final List<String> STAT_FILES = List.of(
            "rx_bytes", "tx_bytes", "rx_packets", "tx_packets"
    );

    /**
     * Reads traffic counters from /sys/class/net/ inside the UPF pod.
     * Two interfaces are read:
     *  - ogstun (TUN device): carries actual UE payload traffic
     *  - n3 (GTP-U interface): carries encapsulated gNB↔UPF traffic
     *
     * Kernel-level counters are always accurate regardless of
     * Open5GS Prometheus metric availability (Issue #2210).
     */
    void scrapeN3InterfaceStats(String tenantId) {
        String upfPodName = findUpfPodName();
        if (upfPodName == null) {
            return;
        }

        long now = System.currentTimeMillis();

        // Read from primary tun interface (ogstun) → upf_rx_bytes, upf_tx_bytes, etc.
        readInterfaceStats(upfPodName, tunInterface, "upf_", "tun", tenantId, now);

        // Read from n3 GTP-U interface → upf_n3_rx_bytes, upf_n3_tx_bytes, etc.
        readInterfaceStats(upfPodName, "n3", "upf_n3_", "n3", tenantId, now);
    }

    private void readInterfaceStats(String podName, String iface, String metricPrefix,
                                    String sourceLabel, String tenantId, long timestamp) {
        for (String stat : STAT_FILES) {
            String value = execInPod(podName,
                    "cat", "/sys/class/net/" + iface + "/statistics/" + stat);

            if (value == null || value.isBlank()) {
                continue;
            }

            try {
                double numericValue = Double.parseDouble(value.trim());

                PmMetric metric = new PmMetric();
                metric.setMetricName(metricPrefix + stat);
                metric.setValue(numericValue);
                metric.setTimestamp(timestamp);
                metric.setMetricType(PmMetric.MetricType.COUNTER);
                metric.setLabels(Map.of("source", sourceLabel, "interface", iface));
                pmService.ingestMetric(tenantId, metric);
            } catch (NumberFormatException e) {
                log.debug("Non-numeric value for {}/{} in UPF pod: {}", iface, stat, value);
            }
        }
    }

    private String findUpfPodName() {
        try {
            String[] labelParts = upfPodLabel.split("=", 2);
            var pods = k8sClient.pods()
                    .inNamespace(k8sNamespace)
                    .withLabel(labelParts[0], labelParts[1])
                    .list()
                    .getItems();

            if (pods.isEmpty()) {
                return null;
            }
            return pods.get(0).getMetadata().getName();
        } catch (Exception e) {
            log.debug("Cannot list UPF pods: {}", e.getMessage());
            return null;
        }
    }

    private String execInPod(String podName, String... command) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             ByteArrayOutputStream err = new ByteArrayOutputStream();
             ExecWatch exec = k8sClient.pods()
                     .inNamespace(k8sNamespace)
                     .withName(podName)
                     .writingOutput(out)
                     .writingError(err)
                     .exec(command)) {

            exec.exitCode().get(5, TimeUnit.SECONDS);

            String result = out.toString(StandardCharsets.UTF_8).trim();
            if (result.isEmpty()) {
                String error = err.toString(StandardCharsets.UTF_8).trim();
                if (!error.isEmpty()) {
                    log.debug("exec error in pod {}: {}", podName, error);
                }
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.debug("Failed to exec in pod {}: {}", podName, e.getMessage());
            return null;
        }
    }
}
