package com.opticoms.optinmscore.domain.suci.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.suci.dto.SuciProfileRequest;
import com.opticoms.optinmscore.domain.suci.dto.SuciProfileResponse;
import com.opticoms.optinmscore.domain.suci.mapper.SuciProfileMapper;
import com.opticoms.optinmscore.domain.suci.model.SuciProfile;
import com.opticoms.optinmscore.domain.suci.service.SuciProfileService;
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
@RequestMapping("/api/v1/suci/profiles")
@RequiredArgsConstructor
@Tag(name = "SUCI Profile Management",
        description = "HNET key pair management for SUPI concealment (3GPP TS 33.501 SUCI)")
public class SuciProfileController {

    private final SuciProfileService suciProfileService;
    private final SuciProfileMapper suciProfileMapper;

    @Operation(summary = "Create a new SUCI profile (ADMIN only)")
    @PostMapping
    public ResponseEntity<SuciProfileResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody SuciProfileRequest profileRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(suciProfileMapper.toResponse(
                        suciProfileService.create(tenantId, suciProfileMapper.toEntity(profileRequest))));
    }

    @Operation(summary = "List SUCI profiles with pagination")
    @GetMapping
    public ResponseEntity<Page<SuciProfileResponse>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "homeNetworkPublicKeyId") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(suciProfileService.list(tenantId, pageable)
                .map(suciProfileMapper::toResponse));
    }

    @Operation(summary = "Get SUCI profile by ID")
    @GetMapping("/{id}")
    public ResponseEntity<SuciProfileResponse> getById(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(suciProfileMapper.toResponse(
                suciProfileService.getById(tenantId, id)));
    }

    @Operation(summary = "Filter profiles by protection scheme (NULL_SCHEME, PROFILE_A, PROFILE_B)")
    @GetMapping("/scheme/{scheme}")
    public ResponseEntity<List<SuciProfileResponse>> listByScheme(
            HttpServletRequest request,
            @PathVariable SuciProfile.ProtectionScheme scheme) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(suciProfileMapper.toResponseList(
                suciProfileService.listByScheme(tenantId, scheme)));
    }

    @Operation(summary = "Filter profiles by key status (ACTIVE, INACTIVE, REVOKED)")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SuciProfileResponse>> listByStatus(
            HttpServletRequest request,
            @PathVariable SuciProfile.KeyStatus status) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(suciProfileMapper.toResponseList(
                suciProfileService.listByStatus(tenantId, status)));
    }

    @Operation(summary = "Update a SUCI profile (cannot update REVOKED profiles)")
    @PutMapping("/{id}")
    public ResponseEntity<SuciProfileResponse> update(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody SuciProfileRequest profileRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(suciProfileMapper.toResponse(
                suciProfileService.update(tenantId, id, suciProfileMapper.toEntity(profileRequest))));
    }

    @Operation(summary = "Delete a SUCI profile")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        suciProfileService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke an HNET key (marks as REVOKED, irreversible)")
    @PostMapping("/{id}/revoke")
    public ResponseEntity<SuciProfileResponse> revoke(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(suciProfileMapper.toResponse(
                suciProfileService.revokeKey(tenantId, id)));
    }

    @Operation(summary = "Get total SUCI profile count")
    @GetMapping("/count")
    public ResponseEntity<Long> count(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(suciProfileService.count(tenantId));
    }
}
