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
    private boolean active = true;
}
