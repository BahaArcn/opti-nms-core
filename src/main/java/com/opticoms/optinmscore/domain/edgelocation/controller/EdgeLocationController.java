package com.opticoms.optinmscore.domain.edgelocation.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.edgelocation.dto.EdgeLocationRequest;
import com.opticoms.optinmscore.domain.edgelocation.dto.EdgeLocationResponse;
import com.opticoms.optinmscore.domain.edgelocation.mapper.EdgeLocationMapper;
import com.opticoms.optinmscore.domain.edgelocation.service.EdgeLocationService;
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
@RequestMapping("/api/v1/edge-locations")
@RequiredArgsConstructor
@Tag(name = "Edge Location Management")
public class EdgeLocationController {

    private final EdgeLocationService edgeLocationService;
    private final EdgeLocationMapper edgeLocationMapper;

    @Operation(summary = "Create a new edge location")
    @PostMapping
    public ResponseEntity<EdgeLocationResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody EdgeLocationRequest edgeLocationRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(edgeLocationMapper.toResponse(
                        edgeLocationService.create(tenantId, edgeLocationMapper.toEntity(edgeLocationRequest))));
    }

    @Operation(summary = "List all edge locations with pagination")
    @GetMapping
    public ResponseEntity<Page<EdgeLocationResponse>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(edgeLocationService.list(tenantId, pageable)
                .map(edgeLocationMapper::toResponse));
    }

    @Operation(summary = "Get edge location by ID")
    @GetMapping("/{id}")
    public ResponseEntity<EdgeLocationResponse> getById(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(edgeLocationMapper.toResponse(
                edgeLocationService.getById(tenantId, id)));
    }

    @Operation(summary = "Update edge location by ID")
    @PutMapping("/{id}")
    public ResponseEntity<EdgeLocationResponse> update(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody EdgeLocationRequest edgeLocationRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(edgeLocationMapper.toResponse(
                edgeLocationService.update(tenantId, id, edgeLocationMapper.toEntity(edgeLocationRequest))));
    }

    @Operation(summary = "Delete edge location by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        edgeLocationService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get edge location count")
    @GetMapping("/count")
    public ResponseEntity<Long> count(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(edgeLocationService.count(tenantId));
    }
}
