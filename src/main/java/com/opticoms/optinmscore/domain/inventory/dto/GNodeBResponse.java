package com.opticoms.optinmscore.domain.inventory.dto;

import com.opticoms.optinmscore.domain.inventory.model.GNodeB;
import lombok.Data;

import java.util.List;

@Data
public class GNodeBResponse {
    private String id;
    private String gnbId;
    private String gnbName;
    private GNodeB.ConnectionStatus status;
    private String ipAddress;
    private List<GNodeB.PlmnId> supportedPlmns;
    private List<GNodeB.TaiInfo> supportedTais;
    private GNodeB.SctpInfo sctpInfo;
    private int connectedUeCount;
    private Long lastSeenAt;
    private String edgeLocationId;
    private Long createdAt;
    private Long updatedAt;
}
