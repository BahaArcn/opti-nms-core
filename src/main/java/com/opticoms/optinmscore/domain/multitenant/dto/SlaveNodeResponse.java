package com.opticoms.optinmscore.domain.multitenant.dto;

import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode;
import lombok.Data;

import java.util.Date;

@Data
public class SlaveNodeResponse {
    private String id;
    private String slaveAddress;
    private String slaveTenantId;
    private SlaveNode.SlaveStatus status;
    private Date lastHeartbeat;
    private Date registeredAt;
    private Long createdAt;
    private Long updatedAt;
}
