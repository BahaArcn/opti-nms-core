package com.opticoms.optinmscore.domain.inventory.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "connected_ues")
@CompoundIndex(name = "tenant_imsi_idx", def = "{'tenantId': 1, 'imsi': 1}", unique = true)
public class ConnectedUe extends BaseEntity {

    @Indexed
    private String imsi;

    private String gnbId;

    @Indexed
    private UeStatus status;

    private String ipAddress;

    private String apn;

    private SecurityInfo securityInfo;

    private Long lastSeenAt;

    public enum UeStatus {
        CONNECTED, IDLE, DISCONNECTED
    }

    @Data
    public static class SecurityInfo {
        private String integrityAlgorithm;
        private String cipheringAlgorithm;
    }
}
