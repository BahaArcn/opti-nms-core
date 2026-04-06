package com.opticoms.optinmscore.domain.networkservice.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.networkservice.dto.ServiceInstanceRequest;
import com.opticoms.optinmscore.domain.networkservice.dto.ServiceInstanceResponse;
import com.opticoms.optinmscore.domain.networkservice.mapper.ServiceInstanceMapper;
import com.opticoms.optinmscore.domain.networkservice.service.ServiceInstanceService;
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
@RequestMapping("/api/v1/networks/{networkId}/services")
@RequiredArgsConstructor
@Tag(name = "Service Instance Management")
public class ServiceInstanceController {

    private final ServiceInstanceService serviceInstanceService;
    private final ServiceInstanceMapper serviceInstanceMapper;

    @Operation(summary = "Create a new service instance in a network")
    @PostMapping
    public ResponseEntity<ServiceInstanceResponse> create(
            HttpServletRequest request,
            @PathVariable String networkId,
            @Valid @RequestBody ServiceInstanceRequest serviceRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceInstanceMapper.toResponse(
                        serviceInstanceService.create(tenantId, networkId,
                                serviceInstanceMapper.toEntity(serviceRequest))));
    }

    @Operation(summary = "List service instances in a network with pagination")
    @GetMapping
    public ResponseEntity<Page<ServiceInstanceResponse>> list(
            HttpServletRequest request,
            @PathVariable String networkId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(serviceInstanceService.listByNetwork(tenantId, networkId, pageable)
                .map(serviceInstanceMapper::toResponse));
    }

    @Operation(summary = "Get service instance by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceInstanceResponse> getById(
            HttpServletRequest request,
            @PathVariable String networkId,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(serviceInstanceMapper.toResponse(
                serviceInstanceService.getById(tenantId, id)));
    }

    @Operation(summary = "Update service instance by ID")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceInstanceResponse> update(
            HttpServletRequest request,
            @PathVariable String networkId,
            @PathVariable String id,
            @Valid @RequestBody ServiceInstanceRequest serviceRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(serviceInstanceMapper.toResponse(
                serviceInstanceService.update(tenantId, id,
                        serviceInstanceMapper.toEntity(serviceRequest))));
    }

    @Operation(summary = "Delete service instance by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable String networkId,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        serviceInstanceService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get service instance count for a network")
    @GetMapping("/count")
    public ResponseEntity<Long> count(
            HttpServletRequest request,
            @PathVariable String networkId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(serviceInstanceService.countByNetwork(tenantId, networkId));
    }
}
