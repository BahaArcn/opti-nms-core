package com.opticoms.optinmscore.domain.inventory.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "pdu_sessions")
@CompoundIndex(name = "tenant_session_idx", def = "{'tenantId': 1, 'sessionId': 1}", unique = true)
public class PduSession extends BaseEntity {

    @Indexed
    private String sessionId;

    private String imsi;

    @Indexed
    private String dnn;

    private String ueIpAddress;

    private int sst;

    private String sd;

    private SessionStatus status;

    private Long establishedAt;

    private Long lastSeenAt;

    public enum SessionStatus {
        ACTIVE, RELEASED
    }
}
