package com.opticoms.optinmscore.domain.performance.repository;

import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PmMetricRepository extends MongoRepository<PmMetric, String> {

    /** Range query uses epoch milliseconds for start/end. */
    List<PmMetric> findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
            String tenantId, String metricName, Long startTime, Long endTime);

    PmMetric findFirstByTenantIdAndMetricNameOrderByTimestampDesc(String tenantId, String metricName);

    PmMetric findFirstByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampAsc(
            String tenantId, String metricName, Long startTime, Long endTime);

    PmMetric findFirstByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
            String tenantId, String metricName, Long startTime, Long endTime);

    List<PmMetric> findTop2ByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
            String tenantId, String metricName, Long startTime, Long endTime);
}
