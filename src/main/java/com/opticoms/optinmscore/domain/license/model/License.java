package com.opticoms.optinmscore.domain.license.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "licenses")
@CompoundIndex(name = "tenant_unique_idx", def = "{'tenantId': 1}", unique = true)
@Schema(description = "Tenant license defining resource limits")
public class License extends BaseEntity {

    @Schema(description = "License key (optional, for external tracking)", example = "LIC-2026-ENT-001")
    private String licenseKey;

    @Schema(description = "Maximum number of subscribers (null = unlimited)")
    private Integer maxSubscribers;

    @Schema(description = "Maximum number of gNodeBs (null = unlimited)")
    private Integer maxGNodeBs;

    @Schema(description = "Maximum number of DNN/APN profiles (null = unlimited)")
    private Integer maxDnns;

    @Schema(description = "Maximum number of edge locations (null = unlimited)")
    private Integer maxEdgeLocations;

    @Schema(description = "Maximum number of users (null = unlimited)")
    private Integer maxUsers;

    @Schema(description = "License expiry timestamp in epoch ms (null = perpetual)")
    private Long expiresAt;

    @Schema(description = "Whether this license is active")
    private boolean active = true;

    @Schema(description = "Human-readable description", example = "Enterprise license - 1000 subscribers")
    private String description;
}
