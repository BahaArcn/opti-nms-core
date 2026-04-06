package com.opticoms.optinmscore.domain.performance.dto;

import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class PmMetricRequest {
    @NotBlank
    private String metricName;

    @NotNull
    private Double value;

    private Long timestamp;
    private Map<String, String> labels;
    private PmMetric.MetricType metricType;
}
