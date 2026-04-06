package com.opticoms.optinmscore.domain.inventory.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.inventory.dto.ConnectedUeResponse;
import com.opticoms.optinmscore.domain.inventory.dto.GNodeBResponse;
import com.opticoms.optinmscore.domain.inventory.dto.NodeResourceRequest;
import com.opticoms.optinmscore.domain.inventory.dto.NodeResourceResponse;
import com.opticoms.optinmscore.domain.inventory.dto.PduSessionResponse;
import com.opticoms.optinmscore.domain.inventory.mapper.InventoryMapper;
import com.opticoms.optinmscore.domain.inventory.service.InventoryService;
import com.opticoms.optinmscore.domain.inventory.service.NodeResourceService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final NodeResourceService nodeResourceService;
    private final InventoryMapper inventoryMapper;

    @Operation(summary = "List all gNodeBs with pagination")
    @GetMapping("/gnb")
    public ResponseEntity<Page<GNodeBResponse>> listGNodeBs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastSeenAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(
                inventoryService.getAllGNodeBsPaged(tenantId, pageable).map(inventoryMapper::toGNodeBResponse));
    }

    @Operation(summary = "Get gNodeB details by gNB ID")
    @GetMapping("/gnb/{gnbId}")
    public ResponseEntity<GNodeBResponse> getGNodeB(
            HttpServletRequest request,
            @PathVariable String gnbId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(inventoryMapper.toGNodeBResponse(inventoryService.getGNodeB(tenantId, gnbId)));
    }

    @Operation(summary = "Get total gNodeB count")
    @GetMapping("/gnb/count")
    public ResponseEntity<Long> getGNodeBCount(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(inventoryService.getGNodeBCount(tenantId));
    }

    @Operation(summary = "List all connected UEs with pagination")
    @GetMapping("/ue")
    public ResponseEntity<Page<ConnectedUeResponse>> listConnectedUes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastSeenAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(
                inventoryService.getAllConnectedUesPaged(tenantId, pageable).map(inventoryMapper::toConnectedUeResponse));
    }

    @Operation(summary = "Get total connected UE count")
    @GetMapping("/ue/count")
    public ResponseEntity<Long> getConnectedUeCount(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(inventoryService.getConnectedUeCount(tenantId));
    }

    @Operation(summary = "List all PDU sessions with pagination")
    @GetMapping("/session")
    public ResponseEntity<Page<PduSessionResponse>> listPduSessions(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastSeenAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(
                inventoryService.getAllPduSessionsPaged(tenantId, pageable).map(inventoryMapper::toPduSessionResponse));
    }

    @Operation(summary = "Get active PDU session count")
    @GetMapping("/session/count")
    public ResponseEntity<Long> getActiveSessionCount(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(inventoryService.getActiveSessionCount(tenantId));
    }

    // ============================
    // Node Resource endpoints
    // ============================

    @Operation(summary = "Report node resource usage (upsert by nodeId)")
    @PostMapping("/nodes/resources")
    public ResponseEntity<NodeResourceResponse> reportNodeResource(
            HttpServletRequest request,
            @Valid @RequestBody NodeResourceRequest nodeResourceRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryMapper.toNodeResourceResponse(
                        nodeResourceService.reportNodeResource(
                                tenantId, inventoryMapper.toNodeResourceEntity(nodeResourceRequest))));
    }

    @Operation(summary = "List all node resources with pagination")
    @GetMapping("/nodes/resources")
    public ResponseEntity<Page<NodeResourceResponse>> listNodeResources(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastReportedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(
                nodeResourceService.getNodeResources(tenantId, pageable).map(inventoryMapper::toNodeResourceResponse));
    }

    @Operation(summary = "Get node resource by node ID")
    @GetMapping("/nodes/resources/{nodeId}")
    public ResponseEntity<NodeResourceResponse> getNodeResource(
            HttpServletRequest request,
            @PathVariable String nodeId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(
                inventoryMapper.toNodeResourceResponse(nodeResourceService.getNodeResource(tenantId, nodeId)));
    }
}
