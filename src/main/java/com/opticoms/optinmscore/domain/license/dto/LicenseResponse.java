package com.opticoms.optinmscore.domain.license.dto;

import lombok.Data;

@Data
public class LicenseResponse {
    private String id;
    private String licenseKey;
    private Integer maxSubscribers;
    private Integer maxGNodeBs;
    private Integer maxDnns;
    private Integer maxEdgeLocations;
    private Integer maxUsers;
    private Long expiresAt;
    private boolean active;
    private String description;
    private Long createdAt;
    private Long updatedAt;
}
