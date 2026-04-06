package com.opticoms.optinmscore.domain.performance.dto;

import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import lombok.Data;

import java.util.Map;

@Data
public class PmMetricResponse {
    private String id;
    private String metricName;
    private Double value;
    private Long timestamp;
    private Map<String, String> labels;
    private PmMetric.MetricType metricType;
    private Long createdAt;
    private Long updatedAt;
}
