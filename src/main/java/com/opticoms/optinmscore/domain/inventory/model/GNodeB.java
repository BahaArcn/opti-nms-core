package com.opticoms.optinmscore.domain.inventory.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "gnodebs")
@CompoundIndex(name = "tenant_gnbid_idx", def = "{'tenantId': 1, 'gnbId': 1}", unique = true)
public class GNodeB extends BaseEntity {

    @Indexed
    private String gnbId;

    private String gnbName;

    @Indexed
    private ConnectionStatus status;

    private String ipAddress;

    private List<PlmnId> supportedPlmns;

    private List<TaiInfo> supportedTais;

    private SctpInfo sctpInfo;

    private int connectedUeCount;

    private Long lastSeenAt;

    public enum ConnectionStatus {
        CONNECTED, DISCONNECTED
    }

    @Data
    public static class PlmnId {
        private String mcc;
        private String mnc;
    }

    @Data
    public static class TaiInfo {
        private PlmnId plmn;
        private int tac;
    }

    @Data
    public static class SctpInfo {
        private int port;
        private List<String> addresses;
        private int streams;
    }
}
