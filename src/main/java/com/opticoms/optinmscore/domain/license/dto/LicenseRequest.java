package com.opticoms.optinmscore.domain.license.dto;

import lombok.Data;

@Data
public class LicenseRequest {
    private String licenseKey;
    private Integer maxSubscribers;
    private Integer maxGNodeBs;
    private Integer maxDnns;
    private Integer maxEdgeLocations;
    private Integer maxUsers;
    private Long expiresAt;
    private boolean active = true;
    private String description;
}
