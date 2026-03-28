package com.opticoms.optinmscore.domain.performance.service;

import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.repository.PmMetricRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PmService {

    private final PmMetricRepository pmMetricRepository;
    private final ConnectedUeRepository connectedUeRepository;

    public PmMetric ingestMetric(String tenantId, PmMetric metric) {
        metric.setTenantId(tenantId);
        if (metric.getTimestamp() == null) {
            metric.setTimestamp(System.currentTimeMillis());
        }
        return pmMetricRepository.save(metric);
    }

    public List<PmMetric> getMetricsHistory(String tenantId, String metricName, int durationMinutes) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - ((long) durationMinutes * 60 * 1000);

        return pmMetricRepository.findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                tenantId, metricName, startTime, endTime);
    }

    public Double getCurrentValue(String tenantId, String metricName) {
        PmMetric latest = pmMetricRepository.findFirstByTenantIdAndMetricNameOrderByTimestampDesc(tenantId, metricName);
        return latest != null ? latest.getValue() : 0.0;
    }

    /**
     * #27/#31: Total data transferred in the last N minutes (in GB).
     * Calculates as delta between first and last samples of upf_rx_bytes + upf_tx_bytes.
     */
    public Double getTotalDataGB(String tenantId, int minutes) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - ((long) minutes * 60 * 1000);

        List<PmMetric> rxMetrics = pmMetricRepository
                .findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                        tenantId, "upf_rx_bytes", startTime, endTime);
        List<PmMetric> txMetrics = pmMetricRepository
                .findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                        tenantId, "upf_tx_bytes", startTime, endTime);

        double rxDelta = computeDelta(rxMetrics);
        double txDelta = computeDelta(txMetrics);

        return (rxDelta + txDelta) / (1024.0 * 1024.0 * 1024.0);
    }

    /**
     * #30: Current throughput calculated from the two most recent upf_tx_bytes / upf_rx_bytes samples.
     */
    public ThroughputResult getCurrentThroughput(String tenantId) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (5L * 60 * 1000);

        List<PmMetric> rxMetrics = pmMetricRepository
                .findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                        tenantId, "upf_rx_bytes", startTime, endTime);
        List<PmMetric> txMetrics = pmMetricRepository
                .findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                        tenantId, "upf_tx_bytes", startTime, endTime);

        ThroughputResult result = new ThroughputResult();
        result.setUplinkBps(computeRate(txMetrics));
        result.setDownlinkBps(computeRate(rxMetrics));
        result.setTimestamp(endTime);
        return result;
    }

    /**
     * #32: Get per-gNB traffic estimate.
     * UPF metrics don't natively include gNB labels. We estimate by distributing
     * average throughput across connected UEs per gNB.
     */
    public GnbTrafficResult getGnbTraffic(String tenantId, String gnbId, int minutes) {
        List<ConnectedUe> allConnected = connectedUeRepository.findByTenantId(tenantId).stream()
                .filter(ue -> ue.getStatus() == ConnectedUe.UeStatus.CONNECTED)
                .toList();

        long totalConnected = allConnected.size();
        long gnbConnected = allConnected.stream()
                .filter(ue -> gnbId.equals(ue.getGnbId()))
                .count();

        if (totalConnected == 0 || gnbConnected == 0) {
            GnbTrafficResult result = new GnbTrafficResult();
            result.setGnbId(gnbId);
            result.setEstimatedDataGB(0.0);
            result.setConnectedUeCount(gnbConnected);
            result.setTotalNetworkUeCount(totalConnected);
            return result;
        }

        Double totalDataGB = getTotalDataGB(tenantId, minutes);
        double ratio = (double) gnbConnected / totalConnected;

        GnbTrafficResult result = new GnbTrafficResult();
        result.setGnbId(gnbId);
        result.setEstimatedDataGB(totalDataGB * ratio);
        result.setConnectedUeCount(gnbConnected);
        result.setTotalNetworkUeCount(totalConnected);
        return result;
    }

    private double computeDelta(List<PmMetric> metrics) {
        if (metrics == null || metrics.size() < 2) {
            return 0.0;
        }
        double newest = metrics.get(0).getValue();
        double oldest = metrics.get(metrics.size() - 1).getValue();
        double delta = newest - oldest;
        return Math.max(delta, 0.0);
    }

    private double computeRate(List<PmMetric> metrics) {
        if (metrics == null || metrics.size() < 2) {
            return 0.0;
        }
        PmMetric newest = metrics.get(0);
        PmMetric secondNewest = metrics.get(1);
        double byteDiff = newest.getValue() - secondNewest.getValue();
        double timeDiffSec = (newest.getTimestamp() - secondNewest.getTimestamp()) / 1000.0;
        if (timeDiffSec <= 0) return 0.0;
        double bps = (byteDiff * 8.0) / timeDiffSec;
        return Math.max(bps, 0.0);
    }

    @Data
    public static class ThroughputResult {
        private double uplinkBps;
        private double downlinkBps;
        private long timestamp;
    }

    @Data
    public static class GnbTrafficResult {
        private String gnbId;
        private double estimatedDataGB;
        private long connectedUeCount;
        private long totalNetworkUeCount;
    }
}