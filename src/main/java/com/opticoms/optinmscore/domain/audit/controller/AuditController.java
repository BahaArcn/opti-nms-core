package com.opticoms.optinmscore.domain.audit.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.audit.dto.AuditLogResponse;
import com.opticoms.optinmscore.domain.audit.mapper.AuditLogMapper;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.audit.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/v1/audit/logs")
@Tag(name = "Audit Log")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final AuditLogMapper auditLogMapper;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), "timestamp");
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(auditService.list(tenantId, pageable)
                .map(auditLogMapper::toResponse));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLogResponse>> listByUser(
            HttpServletRequest request,
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.listByUserId(tenantId, userId, pageable)
                .map(auditLogMapper::toResponse));
    }

    @GetMapping("/entity/{entityType}")
    public ResponseEntity<Page<AuditLogResponse>> listByEntityType(
            HttpServletRequest request,
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.listByEntityType(tenantId, entityType, pageable)
                .map(auditLogMapper::toResponse));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<Page<AuditLogResponse>> listByAction(
            HttpServletRequest request,
            @PathVariable AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.listByAction(tenantId, action, pageable)
                .map(auditLogMapper::toResponse));
    }

    @GetMapping("/range")
    public ResponseEntity<Page<AuditLogResponse>> listByTimeRange(
            HttpServletRequest request,
            @RequestParam Long startMs,
            @RequestParam Long endMs,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.listByTimeRange(tenantId, new Date(startMs), new Date(endMs), pageable)
                .map(auditLogMapper::toResponse));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(auditService.count(tenantId));
    }
}
