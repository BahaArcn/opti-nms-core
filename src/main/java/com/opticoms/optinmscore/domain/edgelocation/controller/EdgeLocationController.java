package com.opticoms.optinmscore.domain.edgelocation.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.edgelocation.model.EdgeLocation;
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

    @Operation(summary = "Create a new edge location")
    @PostMapping
    public ResponseEntity<EdgeLocation> create(
            HttpServletRequest request,
            @Valid @RequestBody EdgeLocation edgeLocation) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(edgeLocationService.create(tenantId, edgeLocation));
    }

    @Operation(summary = "List all edge locations with pagination")
    @GetMapping
    public ResponseEntity<Page<EdgeLocation>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(edgeLocationService.list(tenantId, pageable));
    }

    @Operation(summary = "Get edge location by ID")
    @GetMapping("/{id}")
    public ResponseEntity<EdgeLocation> getById(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(edgeLocationService.getById(tenantId, id));
    }

    @Operation(summary = "Update edge location by ID")
    @PutMapping("/{id}")
    public ResponseEntity<EdgeLocation> update(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody EdgeLocation edgeLocation) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(edgeLocationService.update(tenantId, id, edgeLocation));
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
