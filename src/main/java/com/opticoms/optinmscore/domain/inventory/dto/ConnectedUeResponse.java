package com.opticoms.optinmscore.domain.inventory.dto;

import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import lombok.Data;

@Data
public class ConnectedUeResponse {
    private String id;
    private String imsi;
    private String gnbId;
    private ConnectedUe.UeStatus status;
    private String ipAddress;
    private String apn;
    private ConnectedUe.SecurityInfo securityInfo;
    private Long lastSeenAt;
    private Long createdAt;
    private Long updatedAt;
}
