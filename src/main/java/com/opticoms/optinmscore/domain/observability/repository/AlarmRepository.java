package com.opticoms.optinmscore.domain.observability.repository;

import com.opticoms.optinmscore.domain.observability.model.Alarm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlarmRepository extends MongoRepository<Alarm, String> {

    Optional<Alarm> findByTenantIdAndSourceAndAlarmTypeAndStatus(
            String tenantId, String source, String alarmType, Alarm.AlarmStatus status);

    List<Alarm> findByTenantIdAndSeverityAndStatusOrderByEventTimeDesc(
            String tenantId, Alarm.Severity severity, Alarm.AlarmStatus status);

    List<Alarm> findByTenantIdAndSeverityOrderByEventTimeDesc(
            String tenantId, Alarm.Severity severity);

    List<Alarm> findByTenantIdAndStatusOrderByEventTimeDesc(
            String tenantId, Alarm.AlarmStatus status);

    List<Alarm> findByTenantIdOrderByEventTimeDesc(String tenantId);

    Page<Alarm> findByTenantIdAndSeverityAndStatusOrderByEventTimeDesc(
            String tenantId, Alarm.Severity severity, Alarm.AlarmStatus status, Pageable pageable);

    Page<Alarm> findByTenantIdAndSeverityOrderByEventTimeDesc(
            String tenantId, Alarm.Severity severity, Pageable pageable);

    Page<Alarm> findByTenantIdAndStatusOrderByEventTimeDesc(
            String tenantId, Alarm.AlarmStatus status, Pageable pageable);

    Page<Alarm> findByTenantIdOrderByEventTimeDesc(String tenantId, Pageable pageable);

    Optional<Alarm> findByIdAndTenantId(String id, String tenantId);

    Page<Alarm> findByTenantId(String tenantId, Pageable pageable);

    long countByTenantIdAndStatus(String tenantId, Alarm.AlarmStatus status);

    long countByTenantIdAndSeverityAndStatus(String tenantId, Alarm.Severity severity, Alarm.AlarmStatus status);
}