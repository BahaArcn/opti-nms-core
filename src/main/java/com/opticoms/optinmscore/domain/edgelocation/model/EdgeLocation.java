package com.opticoms.optinmscore.domain.edgelocation.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "edge_locations")
@CompoundIndex(name = "tenant_name_idx", def = "{'tenantId': 1, 'name': 1}", unique = true)
@Schema(description = "Edge location / site representing a physical deployment point")
public class EdgeLocation extends BaseEntity {

    @NotBlank
    @Schema(description = "Unique edge location name within tenant", example = "Istanbul-DC-1")
    private String name;

    @Schema(description = "Human-readable description", example = "Primary data center in Istanbul")
    private String description;

    @Schema(description = "Physical address", example = "Levent, Istanbul, Turkey")
    private String address;

    @Schema(description = "GPS latitude", example = "41.0082")
    private Double latitude;

    @Schema(description = "GPS longitude", example = "28.9784")
    private Double longitude;

    @Schema(description = "Edge location status")
    private EdgeLocationStatus status = EdgeLocationStatus.ACTIVE;

    public enum EdgeLocationStatus {
        ACTIVE, INACTIVE
    }
}
