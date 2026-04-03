package com.opticoms.optinmscore.domain.license.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.license.model.License;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/licenses")
@RequiredArgsConstructor
@Tag(name = "License Management")
public class LicenseController {

    private final LicenseService licenseService;

    @Operation(summary = "Get current tenant license")
    @GetMapping
    public ResponseEntity<License> getLicense(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(licenseService.getLicense(tenantId));
    }

    @Operation(summary = "Get license status with current usage counts")
    @GetMapping("/status")
    public ResponseEntity<LicenseService.LicenseStatus> getLicenseStatus(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(licenseService.getLicenseStatus(tenantId));
    }
}
