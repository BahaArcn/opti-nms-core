package com.opticoms.optinmscore.domain.system.controller;

import com.opticoms.optinmscore.domain.license.model.License;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/licenses")
@RequiredArgsConstructor
@Tag(name = "License Management")
public class SystemLicenseController {

    private final LicenseService licenseService;

    @Operation(summary = "Create or update license for a tenant (SUPER_ADMIN only)")
    @PostMapping
    public ResponseEntity<License> createOrUpdate(
            @Parameter(description = "Tenant ID", example = "TURK-0001/0001/01")
            @RequestParam String tenantId,
            @Valid @RequestBody License license) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(licenseService.createOrUpdateLicense(tenantId, license));
    }

    @Operation(summary = "Delete license for a tenant (SUPER_ADMIN only)")
    @DeleteMapping
    public ResponseEntity<Void> deleteLicense(
            @Parameter(description = "Tenant ID", example = "TURK-0001/0001/01")
            @RequestParam String tenantId) {
        licenseService.deleteLicense(tenantId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get license for a tenant (SUPER_ADMIN only)")
    @GetMapping
    public ResponseEntity<License> getLicense(
            @Parameter(description = "Tenant ID", example = "TURK-0001/0001/01")
            @RequestParam String tenantId) {
        return ResponseEntity.ok(licenseService.getLicense(tenantId));
    }

    @Operation(summary = "Get license status with usage counts for a tenant (SUPER_ADMIN only)")
    @GetMapping("/status")
    public ResponseEntity<LicenseService.LicenseStatus> getLicenseStatus(
            @Parameter(description = "Tenant ID", example = "TURK-0001/0001/01")
            @RequestParam String tenantId) {
        return ResponseEntity.ok(licenseService.getLicenseStatus(tenantId));
    }
}
