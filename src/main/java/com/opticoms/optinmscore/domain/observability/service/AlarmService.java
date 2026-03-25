package com.opticoms.optinmscore.domain.observability.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.observability.model.Alarm;
import com.opticoms.optinmscore.domain.observability.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;

    /**
     * Raises an alarm. Returns the alarm and a boolean indicating if it was newly created.
     * If a matching active alarm already exists (deduplication), it is updated instead.
     *
     * @return RaiseResult containing the alarm and whether it was newly created
     */
    @Audited(action = AuditAction.CREATE, entityType = "Alarm")
    public RaiseResult raiseAlarm(String tenantId, Alarm alarm) {
        alarm.setTenantId(tenantId);

        Optional<Alarm> existingActiveAlarm = alarmRepository.findByTenantIdAndSourceAndAlarmTypeAndStatus(
                tenantId, alarm.getSource(), alarm.getAlarmType(), Alarm.AlarmStatus.ACTIVE);

        if (existingActiveAlarm.isPresent()) {
            Alarm existing = existingActiveAlarm.get();
            existing.setDescription(alarm.getDescription());
            return new RaiseResult(alarmRepository.save(existing), false);
        }

        alarm.setStatus(Alarm.AlarmStatus.ACTIVE);
        try {
            return new RaiseResult(alarmRepository.save(alarm), true);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            return alarmRepository
                    .findByTenantIdAndSourceAndAlarmTypeAndStatus(
                            tenantId, alarm.getSource(), alarm.getAlarmType(),
                            Alarm.AlarmStatus.ACTIVE)
                    .map(existing -> new RaiseResult(existing, false))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Alarm dedup conflict but active alarm not found"));
        }
    }

    public record RaiseResult(Alarm alarm, boolean created) {}

    @Audited(action = AuditAction.CLEAR, entityType = "Alarm")
    public void clearAlarm(String tenantId, String source, String alarmType) {
        Optional<Alarm> activeAlarm = alarmRepository.findByTenantIdAndSourceAndAlarmTypeAndStatus(
                tenantId, source, alarmType, Alarm.AlarmStatus.ACTIVE);

        if (activeAlarm.isPresent()) {
            Alarm alarm = activeAlarm.get();
            alarm.setStatus(Alarm.AlarmStatus.CLEARED);
            alarm.setClearedTime(System.currentTimeMillis());
            alarm.setClearedBy(getCurrentUsername());

            alarmRepository.save(alarm);
        }
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "system";
    }

    public List<Alarm> getAlarms(String tenantId, Alarm.Severity severity, Alarm.AlarmStatus status) {
        if (severity != null && status != null) {
            return alarmRepository.findByTenantIdAndSeverityAndStatusOrderByEventTimeDesc(tenantId, severity, status);
        } else if (severity != null) {
            return alarmRepository.findByTenantIdAndSeverityOrderByEventTimeDesc(tenantId, severity);
        } else if (status != null) {
            return alarmRepository.findByTenantIdAndStatusOrderByEventTimeDesc(tenantId, status);
        } else {
            return alarmRepository.findByTenantIdOrderByEventTimeDesc(tenantId);
        }
    }

    public Alarm getAlarmById(String tenantId, String alarmId) {
        return alarmRepository.findByIdAndTenantId(alarmId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Alarm not found: " + alarmId));
    }

    public long countActiveAlarms(String tenantId) {
        return alarmRepository.countByTenantIdAndStatus(tenantId, Alarm.AlarmStatus.ACTIVE);
    }

    public long countBySeverity(String tenantId, Alarm.Severity severity) {
        return alarmRepository.countByTenantIdAndSeverityAndStatus(tenantId, severity, Alarm.AlarmStatus.ACTIVE);
    }
}