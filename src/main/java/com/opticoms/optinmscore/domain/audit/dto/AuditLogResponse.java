package com.opticoms.optinmscore.domain.audit.dto;

import com.opticoms.optinmscore.domain.audit.model.AuditLog;
import lombok.Data;

import java.util.Date;

@Data
public class AuditLogResponse {
    private String id;
    private String userId;
    private String username;
    private AuditLog.AuditAction action;
    private String entityType;
    private String entityId;
    private String description;
    private String httpMethod;
    private String requestUri;
    private AuditLog.AuditOutcome outcome;
    private String failureReason;
    private Date timestamp;
}
