package com.opticoms.optinmscore.domain.audit.service;

import com.opticoms.optinmscore.domain.audit.model.AuditLog;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    public void log(AuditLog entry) {
        try {
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to save audit log: {}", e.getMessage());
        }
    }

    public Page<AuditLog> list(String tenantId, Pageable pageable) {
        return repository.findByTenantId(tenantId, pageable);
    }

    public Page<AuditLog> listByUserId(String tenantId, String userId, Pageable pageable) {
        return repository.findByTenantIdAndUserId(tenantId, userId, pageable);
    }

    public Page<AuditLog> listByEntityType(String tenantId, String entityType, Pageable pageable) {
        return repository.findByTenantIdAndEntityType(tenantId, entityType, pageable);
    }

    public Page<AuditLog> listByAction(String tenantId, AuditAction action, Pageable pageable) {
        return repository.findByTenantIdAndAction(tenantId, action, pageable);
    }

    public Page<AuditLog> listByTimeRange(String tenantId, Date start, Date end, Pageable pageable) {
        return repository.findByTenantIdAndTimestampBetween(tenantId, start, end, pageable);
    }

    public long count(String tenantId) {
        return repository.countByTenantId(tenantId);
    }
}
