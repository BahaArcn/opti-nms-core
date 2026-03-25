package com.opticoms.optinmscore.common.util;

import com.opticoms.optinmscore.common.exception.UnauthorizedAccessException;
import jakarta.servlet.http.HttpServletRequest;

public final class TenantContext {

    private TenantContext() {}

    public static String getCurrentTenantId(HttpServletRequest request) {
        String tenantId = (String) request.getAttribute("tenantId");
        if (tenantId == null || tenantId.isBlank()) {
            throw new UnauthorizedAccessException("Tenant context not available");
        }
        return tenantId;
    }
}
