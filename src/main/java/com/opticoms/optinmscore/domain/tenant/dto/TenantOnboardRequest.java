package com.opticoms.optinmscore.domain.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantOnboardRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Z]{4}-\\d{4}/\\d{4}/\\d{2}$",
            message = "Tenant ID must follow format: XXXX-DDDD/DDDD/DD")
    private String tenantId;

    @NotBlank
    private String name;

    @NotBlank
    private String amfUrl;

    @NotBlank
    private String smfUrl;

    @NotBlank
    private String adminUsername;

    @NotBlank @Email
    private String adminEmail;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String adminPassword;

    private String open5gsMongoUri;
    private String upfMetricsUrl;
}
