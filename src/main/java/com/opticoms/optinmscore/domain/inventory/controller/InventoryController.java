package com.opticoms.optinmscore.domain.inventory.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.inventory.model.GNodeB;
import com.opticoms.optinmscore.domain.inventory.model.PduSession;
import com.opticoms.optinmscore.domain.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "List all gNodeBs with pagination")
    @GetMapping("/gnb")
    public ResponseEntity<Page<GNodeB>> listGNodeBs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastSeenAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(inventoryService.getAllGNodeBsPaged(tenantId, pageable));
    }

    @Operation(summary = "Get gNodeB details by gNB ID")
    @GetMapping("/gnb/{gnbId}")
    public ResponseEntity<GNodeB> getGNodeB(
            HttpServletRequest request,
            @PathVariable String gnbId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(inventoryService.getGNodeB(tenantId, gnbId));
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
    public ResponseEntity<Page<ConnectedUe>> listConnectedUes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastSeenAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(inventoryService.getAllConnectedUesPaged(tenantId, pageable));
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
    public ResponseEntity<Page<PduSession>> listPduSessions(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastSeenAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(inventoryService.getAllPduSessionsPaged(tenantId, pageable));
    }

    @Operation(summary = "Get active PDU session count")
    @GetMapping("/session/count")
    public ResponseEntity<Long> getActiveSessionCount(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(inventoryService.getActiveSessionCount(tenantId));
    }
}
