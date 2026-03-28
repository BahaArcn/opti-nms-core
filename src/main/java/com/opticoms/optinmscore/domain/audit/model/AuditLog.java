package com.opticoms.optinmscore.domain.audit.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "audit_logs")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_time_idx", def = "{'tenantId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "tenant_user_idx", def = "{'tenantId': 1, 'userId': 1}")
})
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String userId;
    private String username;

    private AuditAction action;
    private String entityType;
    private String entityId;
    private String description;

    private String ipAddress;
    private String httpMethod;
    private String requestUri;

    private AuditOutcome outcome;
    private String failureReason;

    @Indexed(expireAfterSeconds = 7776000)
    private Date timestamp = new Date();

    public enum AuditAction {
        CREATE, UPDATE, DELETE, REVOKE, DEPRECATE, READ_SENSITIVE, APPLY, REMOVE,
        CHANGE_PASSWORD, RESET_PASSWORD, STATUS_CHANGE, CLEAR, ACKNOWLEDGE
    }

    public enum AuditOutcome {
        SUCCESS, FAILURE
    }
}
