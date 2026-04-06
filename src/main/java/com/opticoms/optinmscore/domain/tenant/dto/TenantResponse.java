package com.opticoms.optinmscore.domain.tenant.dto;

import lombok.Data;

@Data
public class TenantResponse {
    private String id;
    private String tenantId;
    private String name;
    private String amfUrl;
    private String smfUrl;
    private String upfMetricsUrl;
    private boolean active;
    private Long createdAt;
    private Long updatedAt;
}
