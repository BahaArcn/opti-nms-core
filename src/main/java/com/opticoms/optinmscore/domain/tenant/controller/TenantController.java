package com.opticoms.optinmscore.domain.tenant.controller;

import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.UserService;
import com.opticoms.optinmscore.domain.tenant.dto.TenantOnboardRequest;
import com.opticoms.optinmscore.domain.tenant.dto.TenantOnboardResponse;
import com.opticoms.optinmscore.domain.tenant.dto.TenantRequest;
import com.opticoms.optinmscore.domain.tenant.dto.TenantResponse;
import com.opticoms.optinmscore.domain.tenant.mapper.TenantMapper;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/system/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "SUPER_ADMIN only: manage tenants and onboarding")
public class TenantController {

    private final TenantService tenantService;
    private final UserService userService;
    private final TenantMapper tenantMapper;

    @Operation(summary = "Onboard a new tenant with its initial admin user")
    @PostMapping
    public ResponseEntity<TenantOnboardResponse> onboardTenant(
            @Valid @RequestBody TenantOnboardRequest request) {

        Tenant tenant = new Tenant();
        tenant.setTenantId(request.getTenantId());
        tenant.setName(request.getName());
        tenant.setAmfUrl(request.getAmfUrl());
        tenant.setSmfUrl(request.getSmfUrl());
        tenant.setOpen5gsMongoUri(request.getOpen5gsMongoUri());
        tenant.setUpfMetricsUrl(request.getUpfMetricsUrl());
        tenant.setNrfUrl(request.getNrfUrl());
        tenant.setNssfUrl(request.getNssfUrl());
        tenant.setScpUrl(request.getScpUrl());
        tenant.setAusfUrl(request.getAusfUrl());
        tenant.setUdmUrl(request.getUdmUrl());
        tenant.setUdrUrl(request.getUdrUrl());
        tenant.setBsfUrl(request.getBsfUrl());
        tenant.setPcfUrl(request.getPcfUrl());
        Tenant saved = tenantService.createTenant(tenant);

        User admin;
        try {
            admin = userService.createUser(
                    request.getTenantId(),
                    request.getAdminUsername(),
                    request.getAdminEmail(),
                    request.getAdminPassword(),
                    User.Role.ADMIN
            );
        } catch (Exception e) {
            try {
                tenantService.hardDeleteTenant(saved.getTenantId());
                log.warn("Onboarding failed for tenant {}, rolled back tenant creation: {}",
                        saved.getTenantId(), e.getMessage());
            } catch (Exception rollbackEx) {
                log.error("CRITICAL: Tenant {} created but admin user creation failed AND " +
                        "tenant rollback failed. Manual cleanup required.",
                        saved.getTenantId(), rollbackEx);
            }
            throw e;
        }

        TenantOnboardResponse response = new TenantOnboardResponse();
        response.setTenant(tenantMapper.toResponse(saved));
        response.setAdminUsername(admin.getUsername());
        response.setAdminEmail(admin.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List all tenants")
    @GetMapping
    public ResponseEntity<List<TenantResponse>> listTenants() {
        return ResponseEntity.ok(tenantMapper.toResponseList(tenantService.listTenants()));
    }

    @Operation(summary = "Get tenant by MongoDB document id")
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String id) {
        return ResponseEntity.ok(tenantMapper.toResponse(tenantService.getTenantById(id)));
    }

    @Operation(summary = "Update tenant by MongoDB document id")
    @PutMapping("/{id}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable String id,
            @Valid @RequestBody TenantRequest tenantRequest) {
        Tenant existing = tenantService.getTenantById(id);
        Tenant entity = tenantMapper.toEntity(tenantRequest);
        return ResponseEntity.ok(tenantMapper.toResponse(
                tenantService.updateTenant(existing.getTenantId(), entity)));
    }

    @Operation(summary = "Deactivate tenant (soft delete) by MongoDB document id")
    @DeleteMapping("/{id}")
    public ResponseEntity<TenantResponse> deactivateTenant(@PathVariable String id) {
        Tenant existing = tenantService.getTenantById(id);
        return ResponseEntity.ok(tenantMapper.toResponse(
                tenantService.deactivateTenant(existing.getTenantId())));
    }
}
