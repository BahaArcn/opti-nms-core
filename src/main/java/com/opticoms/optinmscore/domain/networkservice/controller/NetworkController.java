package com.opticoms.optinmscore.domain.networkservice.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkRequest;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkResponse;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkSummaryResponse;
import com.opticoms.optinmscore.domain.networkservice.mapper.NetworkMapper;
import com.opticoms.optinmscore.domain.networkservice.service.NetworkGroupService;
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

@RestController
@RequestMapping("/api/v1/networks")
@RequiredArgsConstructor
@Tag(name = "Network Management")
public class NetworkController {

    private final NetworkGroupService networkGroupService;
    private final NetworkMapper networkMapper;

    @Operation(summary = "Create a new network")
    @PostMapping
    public ResponseEntity<NetworkResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody NetworkRequest networkRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(networkMapper.toResponse(
                        networkGroupService.create(tenantId, networkMapper.toEntity(networkRequest))));
    }

    @Operation(summary = "List all networks with pagination")
    @GetMapping
    public ResponseEntity<Page<NetworkResponse>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(networkGroupService.list(tenantId, pageable)
                .map(networkMapper::toResponse));
    }

    @Operation(summary = "Get network by ID")
    @GetMapping("/{id}")
    public ResponseEntity<NetworkResponse> getById(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkMapper.toResponse(
                networkGroupService.getById(tenantId, id)));
    }

    @Operation(summary = "Update network by ID")
    @PutMapping("/{id}")
    public ResponseEntity<NetworkResponse> update(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody NetworkRequest networkRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkMapper.toResponse(
                networkGroupService.update(tenantId, id, networkMapper.toEntity(networkRequest))));
    }

    @Operation(summary = "Delete network and all its service instances")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        networkGroupService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get network summary (service counts by status)")
    @GetMapping("/{id}/summary")
    public ResponseEntity<NetworkSummaryResponse> getSummary(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkGroupService.getSummary(tenantId, id));
    }

    @Operation(summary = "Get total network count")
    @GetMapping("/count")
    public ResponseEntity<Long> count(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkGroupService.count(tenantId));
    }
}
