package com.opticoms.optinmscore.domain.observability.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.observability.dto.AlarmRequest;
import com.opticoms.optinmscore.domain.observability.dto.AlarmResponse;
import com.opticoms.optinmscore.domain.observability.mapper.AlarmMapper;
import com.opticoms.optinmscore.domain.observability.model.Alarm;
import com.opticoms.optinmscore.domain.observability.service.AlarmService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fault/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;
    private final AlarmMapper alarmMapper;

    @Operation(summary = "List alarms with optional severity and status filters (paginated)")
    @GetMapping
    public ResponseEntity<Page<AlarmResponse>> getAlarms(
            HttpServletRequest request,
            @RequestParam(required = false) Alarm.Severity severity,
            @RequestParam(required = false) Alarm.AlarmStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "eventTime") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(
                alarmService.getAlarms(tenantId, severity, status, pageable).map(alarmMapper::toResponse));
    }

    @Operation(summary = "Get alarm by ID")
    @GetMapping("/{alarmId}")
    public ResponseEntity<AlarmResponse> getAlarmById(
            HttpServletRequest request,
            @PathVariable String alarmId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(alarmMapper.toResponse(alarmService.getAlarmById(tenantId, alarmId)));
    }

    @Operation(summary = "Raise a new alarm (deduplicated by source + alarmType)")
    @PostMapping
    public ResponseEntity<AlarmResponse> raiseAlarm(
            HttpServletRequest request,
            @Valid @RequestBody AlarmRequest alarmRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        AlarmService.RaiseResult result = alarmService.raiseAlarm(tenantId, alarmMapper.toEntity(alarmRequest));
        if (result.created()) {
            return ResponseEntity.status(201).body(alarmMapper.toResponse(result.alarm()));
        }
        return ResponseEntity.ok(alarmMapper.toResponse(result.alarm()));
    }

    @Operation(summary = "Acknowledge an active alarm")
    @PutMapping("/{alarmId}/acknowledge")
    public ResponseEntity<AlarmResponse> acknowledgeAlarm(
            HttpServletRequest request,
            @PathVariable String alarmId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(alarmMapper.toResponse(alarmService.acknowledgeAlarm(tenantId, alarmId)));
    }

    @Operation(summary = "Clear an active or acknowledged alarm by source and alarmType")
    @PostMapping("/clear")
    public ResponseEntity<Void> clearAlarm(
            HttpServletRequest request,
            @RequestParam String source,
            @RequestParam String alarmType) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        alarmService.clearAlarm(tenantId, source, alarmType);
        return ResponseEntity.ok().build();
    }
}
