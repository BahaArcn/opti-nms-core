package com.opticoms.optinmscore.domain.audit.repository;

import com.opticoms.optinmscore.domain.audit.model.AuditLog;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByTenantId(String tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndUserId(String tenantId, String userId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndEntityType(String tenantId, String entityType, Pageable pageable);

    Page<AuditLog> findByTenantIdAndAction(String tenantId, AuditAction action, Pageable pageable);

    Page<AuditLog> findByTenantIdAndTimestampBetween(String tenantId, Date start, Date end, Pageable pageable);

    long countByTenantId(String tenantId);
}
