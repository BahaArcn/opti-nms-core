package com.opticoms.optinmscore.domain.performance.service;

import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.repository.PmMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PmService {

    private final PmMetricRepository pmMetricRepository;

    public PmMetric ingestMetric(String tenantId, PmMetric metric) {
        metric.setTenantId(tenantId);
        if (metric.getTimestamp() == null) {
            metric.setTimestamp(System.currentTimeMillis()); // Şu anki zaman (Long)
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
}