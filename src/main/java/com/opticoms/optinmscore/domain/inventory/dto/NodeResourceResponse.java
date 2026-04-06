package com.opticoms.optinmscore.domain.inventory.dto;

import com.opticoms.optinmscore.domain.inventory.model.NodeResource;
import lombok.Data;

@Data
public class NodeResourceResponse {
    private String id;
    private String nodeId;
    private String nodeName;
    private String edgeLocationId;
    private NodeResource.NodeStatus status;
    private Double cpuPercent;
    private Double memoryPercent;
    private Double diskPercent;
    private Long lastReportedAt;
    private Long createdAt;
    private Long updatedAt;
}
