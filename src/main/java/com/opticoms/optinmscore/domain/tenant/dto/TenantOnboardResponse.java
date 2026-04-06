package com.opticoms.optinmscore.domain.tenant.dto;

import lombok.Data;

@Data
public class TenantOnboardResponse {
    private TenantResponse tenant;
    private String adminUsername;
    private String adminEmail;
}
