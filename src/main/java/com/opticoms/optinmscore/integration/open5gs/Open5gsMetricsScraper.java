package com.opticoms.optinmscore.integration.open5gs;

import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.service.PmService;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes Prometheus-format metrics from Open5GS UPF and stores them as PmMetric documents.
 * Targets: upf_session_rx_bytes, upf_session_tx_bytes, upf_session_count
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Open5gsMetricsScraper {

    private final TenantRepository tenantRepository;
    private final PmService pmService;
    private final RestTemplate restTemplate;

    private static final Pattern METRIC_LINE_PATTERN =
            Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)(?:\\{([^}]*)})? (.+)$");
    private static final Pattern LABEL_PATTERN =
            Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)=\"([^\"]*)\"");

    private static final List<String> TARGET_METRICS = List.of(
            "upf_session_rx_bytes",
            "upf_session_tx_bytes",
            "upf_session_count"
    );

    @Scheduled(fixedRateString = "${open5gs.metrics.scrape-interval-ms:30000}")
    @SchedulerLock(name = "upf_metrics_scrape", lockAtMostFor = "28s", lockAtLeastFor = "10s")
    public void scrapeUpfMetrics() {
        List<Tenant> tenants = tenantRepository.findByActiveTrue();
        for (Tenant tenant : tenants) {
            if (tenant.getUpfMetricsUrl() == null || tenant.getUpfMetricsUrl().isBlank()) {
                continue;
            }
            try {
                scrapeForTenant(tenant);
            } catch (Exception e) {
                log.warn("Failed to scrape UPF metrics for tenant {}: {}", tenant.getTenantId(), e.getMessage());
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
        metric.setMetricType(metricName.contains("count") ? PmMetric.MetricType.GAUGE : PmMetric.MetricType.COUNTER);

        pmService.ingestMetric(tenantId, metric);
    }

    private String mapMetricName(String prometheusName) {
        return switch (prometheusName) {
            case "upf_session_rx_bytes" -> "upf_rx_bytes";
            case "upf_session_tx_bytes" -> "upf_tx_bytes";
            case "upf_session_count" -> "upf_session_count";
            default -> prometheusName;
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
}
