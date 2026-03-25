package com.opticoms.optinmscore.domain.performance.repository;

import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PmMetricRepository extends MongoRepository<PmMetric, String> {

    // Tarih aralığı sorgusu artık Long (milisaniye) alıyor
    List<PmMetric> findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
            String tenantId, String metricName, Long startTime, Long endTime);

    PmMetric findFirstByTenantIdAndMetricNameOrderByTimestampDesc(String tenantId, String metricName);
}