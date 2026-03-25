package com.opticoms.optinmscore.domain.performance.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "pm_metrics")
@CompoundIndex(name = "tenant_metric_time_idx", def = "{'tenantId': 1, 'metricName': 1, 'timestamp': -1}")
public class PmMetric extends BaseEntity {

    @NotBlank
    @Indexed
    private String metricName;

    @NotNull
    private Double value;

    @Indexed
    private Long timestamp = System.currentTimeMillis();

    private Map<String, String> labels;

    private MetricType metricType;

    public enum MetricType {
        GAUGE, COUNTER
    }
}