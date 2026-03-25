package com.opticoms.optinmscore.domain.apn.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.apn.model.ApnProfile;
import com.opticoms.optinmscore.domain.apn.service.ApnProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/apn/profiles")
@RequiredArgsConstructor
@Tag(name = "APN/DNN Profile Management",
        description = "Data Network Name (DNN) rules and QoS policies for 5G PDU sessions")
public class ApnProfileController {

    private final ApnProfileService apnProfileService;

    @Operation(summary = "Create a new APN/DNN profile (ADMIN only)")
    @PostMapping
    public ResponseEntity<ApnProfile> create(
            HttpServletRequest request,
            @Valid @RequestBody ApnProfile profile) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apnProfileService.create(tenantId, profile));
    }

    @Operation(summary = "List APN/DNN profiles with pagination")
    @GetMapping
    public ResponseEntity<?> list(
            HttpServletRequest request,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "dnn") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        if (Boolean.TRUE.equals(enabled)) {
            return ResponseEntity.ok(apnProfileService.listEnabled(tenantId));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(apnProfileService.list(tenantId, pageable));
    }

    @Operation(summary = "Get APN/DNN profile by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApnProfile> getById(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(apnProfileService.getById(tenantId, id));
    }

    @Operation(summary = "Filter profiles by SST (1=eMBB, 2=uRLLC, 3=MIoT, 4=V2X, 5=HMTC)")
    @GetMapping("/sst/{sst}")
    public ResponseEntity<List<ApnProfile>> listBySst(
            HttpServletRequest request,
            @PathVariable Integer sst) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(apnProfileService.listBySst(tenantId, sst));
    }

    @Operation(summary = "Filter profiles by status (ACTIVE, INACTIVE, DEPRECATED)")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApnProfile>> listByStatus(
            HttpServletRequest request,
            @PathVariable ApnProfile.ProfileStatus status) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(apnProfileService.listByStatus(tenantId, status));
    }

    @Operation(summary = "Update an APN/DNN profile (cannot update DEPRECATED)")
    @PutMapping("/{id}")
    public ResponseEntity<ApnProfile> update(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody ApnProfile profile) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(apnProfileService.update(tenantId, id, profile));
    }

    @Operation(summary = "Delete an APN/DNN profile")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        apnProfileService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deprecate an APN/DNN profile (disables and marks deprecated)")
    @PostMapping("/{id}/deprecate")
    public ResponseEntity<ApnProfile> deprecate(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(apnProfileService.deprecate(tenantId, id));
    }

    @Operation(summary = "Get total APN/DNN profile count")
    @GetMapping("/count")
    public ResponseEntity<Long> count(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(apnProfileService.count(tenantId));
    }
}
