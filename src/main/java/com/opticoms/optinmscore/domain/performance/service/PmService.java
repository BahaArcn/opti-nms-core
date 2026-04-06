package com.opticoms.optinmscore.domain.performance.service;

import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.repository.PmMetricRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class PmService {

    private final PmMetricRepository pmMetricRepository;
    private final ConnectedUeRepository connectedUeRepository;
    private final MongoTemplate mongoTemplate;

    @Value("${pm.metrics.retention-days:30}")
    private int retentionDays;

    private static final Map<String, int[]> RANGE_MAP = Map.of(
            "5m",  new int[]{5, 1},
            "1h",  new int[]{60, 5},
            "24h", new int[]{1440, 60},
            "3d",  new int[]{4320, 480},
            "7d",  new int[]{10080, 720}
    );

    public PmMetric ingestMetric(String tenantId, PmMetric metric) {
        metric.setTenantId(tenantId);
        if (metric.getTimestamp() == null) {
            metric.setTimestamp(System.currentTimeMillis());
        }
        long retentionMs = (long) retentionDays * 24 * 60 * 60 * 1000;
        metric.setExpiresAt(new Date(metric.getTimestamp() + retentionMs));
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
     * Only fetches the oldest and newest sample (2 queries per metric instead of loading all).
     */
    public Double getTotalDataGB(String tenantId, int minutes) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - ((long) minutes * 60 * 1000);

        double rxDelta = computeDeltaFromBounds(tenantId, "upf_rx_bytes", startTime, endTime);
        double txDelta = computeDeltaFromBounds(tenantId, "upf_tx_bytes", startTime, endTime);

        return (rxDelta + txDelta) / (1024.0 * 1024.0 * 1024.0);
    }

    /**
     * #30: Current throughput from the two most recent samples (2 queries total, not full range).
     */
    public ThroughputResult getCurrentThroughput(String tenantId) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (5L * 60 * 1000);

        List<PmMetric> rxTop2 = pmMetricRepository
                .findTop2ByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                        tenantId, "upf_rx_bytes", startTime, endTime);
        List<PmMetric> txTop2 = pmMetricRepository
                .findTop2ByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                        tenantId, "upf_tx_bytes", startTime, endTime);

        ThroughputResult result = new ThroughputResult();
        result.setUplinkBps(computeRate(txTop2));
        result.setDownlinkBps(computeRate(rxTop2));
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

    public TrafficSeriesResponse getTrafficSeries(String tenantId, String range) {
        int[] params = RANGE_MAP.get(range);
        if (params == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid range. Allowed: " + RANGE_MAP.keySet());
        }
        int totalMinutes = params[0];
        int bucketMinutes = params[1];
        long bucketMs = (long) bucketMinutes * 60 * 1000;
        long endTime = System.currentTimeMillis();
        long startTime = endTime - ((long) totalMinutes * 60 * 1000);

        Map<Long, double[]> rxBuckets = runBucketAggregation(tenantId, "upf_rx_bytes", startTime, endTime, bucketMs);
        Map<Long, double[]> txBuckets = runBucketAggregation(tenantId, "upf_tx_bytes", startTime, endTime, bucketMs);

        Set<Long> allTimestamps = new TreeSet<>();
        allTimestamps.addAll(rxBuckets.keySet());
        allTimestamps.addAll(txBuckets.keySet());

        List<TrafficDataPoint> dataPoints = new ArrayList<>();
        for (Long ts : allTimestamps) {
            TrafficDataPoint dp = new TrafficDataPoint();
            dp.setTimestamp(ts);
            double[] rx = rxBuckets.getOrDefault(ts, new double[]{0, 0});
            double[] tx = txBuckets.getOrDefault(ts, new double[]{0, 0});
            dp.setRxBytes(Math.max(rx[1] - rx[0], 0));
            dp.setTxBytes(Math.max(tx[1] - tx[0], 0));
            dataPoints.add(dp);
        }

        TrafficSeriesResponse response = new TrafficSeriesResponse();
        response.setRange(range);
        response.setBucketMinutes(bucketMinutes);
        response.setDataPoints(dataPoints);
        return response;
    }

    /**
     * MongoDB aggregation: group metric values into time buckets.
     * Returns map of bucketTimestamp -> [firstValue, lastValue].
     */
    private Map<Long, double[]> runBucketAggregation(String tenantId, String metricName,
                                                      long startTime, long endTime, long bucketMs) {
        Document matchStage = new Document("$match", new Document()
                .append("tenantId", tenantId)
                .append("metricName", metricName)
                .append("timestamp", new Document("$gte", startTime).append("$lte", endTime)));

        Document sortAscStage = new Document("$sort", new Document("timestamp", 1));

        Document bucketExpr = new Document("$multiply", List.of(
                new Document("$floor", new Document("$divide", List.of("$timestamp", bucketMs))),
                bucketMs));

        Document groupStage = new Document("$group", new Document()
                .append("_id", bucketExpr)
                .append("firstValue", new Document("$first", "$value"))
                .append("lastValue", new Document("$last", "$value")));

        Document sortBucketStage = new Document("$sort", new Document("_id", 1));

        List<Document> pipeline = List.of(matchStage, sortAscStage, groupStage, sortBucketStage);

        List<Document> results = mongoTemplate.getDb()
                .getCollection("pm_metrics")
                .aggregate(pipeline)
                .into(new ArrayList<>());

        Map<Long, double[]> buckets = new LinkedHashMap<>();
        for (Document doc : results) {
            long bucketTs = ((Number) doc.get("_id")).longValue();
            double first = ((Number) doc.get("firstValue")).doubleValue();
            double last = ((Number) doc.get("lastValue")).doubleValue();
            buckets.put(bucketTs, new double[]{first, last});
        }
        return buckets;
    }

    private double computeDeltaFromBounds(String tenantId, String metricName, long startTime, long endTime) {
        PmMetric newest = pmMetricRepository
                .findFirstByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                        tenantId, metricName, startTime, endTime);
        PmMetric oldest = pmMetricRepository
                .findFirstByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampAsc(
                        tenantId, metricName, startTime, endTime);
        if (newest == null || oldest == null) return 0.0;
        return Math.max(newest.getValue() - oldest.getValue(), 0.0);
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

    @Data
    public static class TrafficDataPoint {
        private long timestamp;
        private double rxBytes;
        private double txBytes;
    }

    @Data
    public static class TrafficSeriesResponse {
        private String range;
        private int bucketMinutes;
        private List<TrafficDataPoint> dataPoints;
    }
}