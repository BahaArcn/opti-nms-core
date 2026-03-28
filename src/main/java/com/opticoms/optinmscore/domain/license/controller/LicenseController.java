package com.opticoms.optinmscore.domain.license.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.license.model.License;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/licenses")
@RequiredArgsConstructor
@Tag(name = "License Management")
public class LicenseController {

    private final LicenseService licenseService;

    @Operation(summary = "Create or update tenant license (upsert)")
    @PostMapping
    public ResponseEntity<License> createOrUpdate(
            HttpServletRequest request,
            @Valid @RequestBody License license) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(licenseService.createOrUpdateLicense(tenantId, license));
    }

    @Operation(summary = "Get current tenant license")
    @GetMapping
    public ResponseEntity<License> getLicense(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(licenseService.getLicense(tenantId));
    }

    @Operation(summary = "Delete tenant license")
    @DeleteMapping
    public ResponseEntity<Void> deleteLicense(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        licenseService.deleteLicense(tenantId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get license status with current usage counts")
    @GetMapping("/status")
    public ResponseEntity<LicenseService.LicenseStatus> getLicenseStatus(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(licenseService.getLicenseStatus(tenantId));
    }
}
