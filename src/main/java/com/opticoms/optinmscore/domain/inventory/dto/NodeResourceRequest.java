package com.opticoms.optinmscore.domain.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NodeResourceRequest {
    @NotBlank
    private String nodeId;

    private String nodeName;
    private String edgeLocationId;

    @Min(0) @Max(100)
    private Double cpuPercent;

    @Min(0) @Max(100)
    private Double memoryPercent;

    @Min(0) @Max(100)
    private Double diskPercent;
}
