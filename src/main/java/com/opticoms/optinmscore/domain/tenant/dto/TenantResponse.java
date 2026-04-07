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

    private String nrfUrl;
    private String nssfUrl;
    private String scpUrl;
    private String ausfUrl;
    private String udmUrl;
    private String udrUrl;
    private String bsfUrl;
    private String pcfUrl;

    private boolean active;
    private Long createdAt;
    private Long updatedAt;
}
