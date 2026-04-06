package com.opticoms.optinmscore.domain.certificate.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.certificate.dto.CertificateEntryRequest;
import com.opticoms.optinmscore.domain.certificate.dto.CertificateEntryResponse;
import com.opticoms.optinmscore.domain.certificate.mapper.CertificateEntryMapper;
import com.opticoms.optinmscore.domain.certificate.model.CertificateEntry;
import com.opticoms.optinmscore.domain.certificate.service.CertificateService;
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
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificate Management",
        description = "X.509 certificate lifecycle management for NF TLS/mTLS")
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateEntryMapper certificateEntryMapper;

    @Operation(summary = "Upload / create a new certificate (ADMIN only)")
    @PostMapping
    public ResponseEntity<CertificateEntryResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody CertificateEntryRequest entryRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificateEntryMapper.toResponse(
                        certificateService.create(tenantId, certificateEntryMapper.toEntity(entryRequest))));
    }

    @Operation(summary = "List certificates with pagination")
    @GetMapping
    public ResponseEntity<Page<CertificateEntryResponse>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(certificateService.list(tenantId, pageable)
                .map(certificateEntryMapper::toResponse));
    }

    @Operation(summary = "Get certificate by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CertificateEntryResponse> getById(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(certificateEntryMapper.toResponse(
                certificateService.getById(tenantId, id)));
    }

    @Operation(summary = "Filter certificates by type (SERVER, CLIENT, CA)")
    @GetMapping("/type/{type}")
    public ResponseEntity<List<CertificateEntryResponse>> listByType(
            HttpServletRequest request,
            @PathVariable CertificateEntry.CertType type) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(certificateEntryMapper.toResponseList(
                certificateService.listByType(tenantId, type)));
    }

    @Operation(summary = "Filter certificates by status (ACTIVE, INACTIVE, EXPIRED, REVOKED)")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CertificateEntryResponse>> listByStatus(
            HttpServletRequest request,
            @PathVariable CertificateEntry.CertStatus status) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(certificateEntryMapper.toResponseList(
                certificateService.listByStatus(tenantId, status)));
    }

    @Operation(summary = "List certificates expiring within N days")
    @GetMapping("/expiring")
    public ResponseEntity<List<CertificateEntryResponse>> listExpiring(
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") int withinDays) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(certificateEntryMapper.toResponseList(
                certificateService.listExpiringSoon(tenantId, withinDays)));
    }

    @Operation(summary = "Update a certificate (cannot update REVOKED)")
    @PutMapping("/{id}")
    public ResponseEntity<CertificateEntryResponse> update(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody CertificateEntryRequest entryRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(certificateEntryMapper.toResponse(
                certificateService.update(tenantId, id, certificateEntryMapper.toEntity(entryRequest))));
    }

    @Operation(summary = "Delete a certificate")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        certificateService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Revoke a certificate (irreversible)")
    @PostMapping("/{id}/revoke")
    public ResponseEntity<CertificateEntryResponse> revoke(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(certificateEntryMapper.toResponse(
                certificateService.revoke(tenantId, id)));
    }

    @Operation(summary = "Get total certificate count")
    @GetMapping("/count")
    public ResponseEntity<Long> count(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(certificateService.count(tenantId));
    }
}
