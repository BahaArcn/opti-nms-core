package com.opticoms.optinmscore.domain.system.controller;

import com.opticoms.optinmscore.domain.license.dto.LicenseRequest;
import com.opticoms.optinmscore.domain.license.dto.LicenseResponse;
import com.opticoms.optinmscore.domain.license.mapper.LicenseMapper;
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
    private final LicenseMapper licenseMapper;

    @Operation(summary = "Create or update license for a tenant (SUPER_ADMIN only)")
    @PostMapping
    public ResponseEntity<LicenseResponse> createOrUpdate(
            @Parameter(description = "Tenant ID", example = "TURK-0001/0001/01")
            @RequestParam String tenantId,
            @Valid @RequestBody LicenseRequest licenseRequest) {
        License entity = licenseMapper.toEntity(licenseRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(licenseMapper.toResponse(licenseService.createOrUpdateLicense(tenantId, entity)));
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
    public ResponseEntity<LicenseResponse> getLicense(
            @Parameter(description = "Tenant ID", example = "TURK-0001/0001/01")
            @RequestParam String tenantId) {
        return ResponseEntity.ok(licenseMapper.toResponse(licenseService.getLicense(tenantId)));
    }

    @Operation(summary = "Get license status with usage counts for a tenant (SUPER_ADMIN only)")
    @GetMapping("/status")
    public ResponseEntity<LicenseService.LicenseStatus> getLicenseStatus(
            @Parameter(description = "Tenant ID", example = "TURK-0001/0001/01")
            @RequestParam String tenantId) {
        return ResponseEntity.ok(licenseService.getLicenseStatus(tenantId));
    }
}
