package com.opticoms.optinmscore.domain.observability.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.observability.model.Alarm;
import com.opticoms.optinmscore.domain.observability.service.AlarmService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fault/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @Operation(summary = "List alarms with optional severity and status filters")
    @GetMapping
    public ResponseEntity<List<Alarm>> getAlarms(
            HttpServletRequest request,
            @RequestParam(required = false) Alarm.Severity severity,
            @RequestParam(required = false) Alarm.AlarmStatus status) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(alarmService.getAlarms(tenantId, severity, status));
    }

    @Operation(summary = "Get alarm by ID")
    @GetMapping("/{alarmId}")
    public ResponseEntity<Alarm> getAlarmById(
            HttpServletRequest request,
            @PathVariable String alarmId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(alarmService.getAlarmById(tenantId, alarmId));
    }

    @Operation(summary = "Raise a new alarm (deduplicated by source + alarmType)")
    @PostMapping
    public ResponseEntity<Alarm> raiseAlarm(
            HttpServletRequest request,
            @Valid @RequestBody Alarm alarm) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        AlarmService.RaiseResult result = alarmService.raiseAlarm(tenantId, alarm);
        if (result.created()) {
            return ResponseEntity.status(201).body(result.alarm());
        }
        return ResponseEntity.ok(result.alarm());
    }

    @Operation(summary = "Clear an active alarm by source and alarmType")
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
