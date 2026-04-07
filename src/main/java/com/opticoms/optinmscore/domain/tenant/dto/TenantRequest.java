package com.opticoms.optinmscore.domain.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String amfUrl;

    @NotBlank
    private String smfUrl;

    private String open5gsMongoUri;
    private String upfMetricsUrl;

    private String nrfUrl;
    private String nssfUrl;
    private String scpUrl;
    private String ausfUrl;
    private String udmUrl;
    private String udrUrl;
    private String bsfUrl;
    private String pcfUrl;

    private boolean active = true;
}
