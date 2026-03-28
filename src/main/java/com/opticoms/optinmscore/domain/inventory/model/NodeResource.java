package com.opticoms.optinmscore.domain.inventory.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "node_resources")
@CompoundIndex(name = "tenant_nodeid_idx", def = "{'tenantId': 1, 'nodeId': 1}", unique = true)
@Schema(description = "Node resource usage report (CPU, memory, disk)")
public class NodeResource extends BaseEntity {

    @NotBlank
    @Schema(description = "Unique node identifier within tenant", example = "node-amf-01")
    private String nodeId;

    @Schema(description = "Human-readable node name", example = "AMF Server 1")
    private String nodeName;

    @Schema(description = "Optional reference to an EdgeLocation document ID")
    private String edgeLocationId;

    @Schema(description = "Node health status (auto-calculated from resource usage)", accessMode = Schema.AccessMode.READ_ONLY)
    private NodeStatus status;

    @Min(0) @Max(100)
    @Schema(description = "CPU usage percentage", example = "45.2")
    private Double cpuPercent;

    @Min(0) @Max(100)
    @Schema(description = "Memory usage percentage", example = "62.8")
    private Double memoryPercent;

    @Min(0) @Max(100)
    @Schema(description = "Disk usage percentage", example = "38.5")
    private Double diskPercent;

    @Schema(description = "Last reported timestamp (epoch ms)", accessMode = Schema.AccessMode.READ_ONLY)
    private Long lastReportedAt;

    public enum NodeStatus {
        HEALTHY, WARNING, CRITICAL
    }
}
