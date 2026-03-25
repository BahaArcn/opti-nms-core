package com.opticoms.optinmscore.domain.multitenant.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode.SlaveStatus;
import com.opticoms.optinmscore.domain.multitenant.service.MasterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/master/slaves")
@Tag(name = "Master - Slave Management")
@RequiredArgsConstructor
public class MasterController {

    private final MasterService masterService;

    @PostMapping("/register")
    public ResponseEntity<SlaveNode> registerSlave(
            HttpServletRequest request,
            @Valid @RequestBody RegisterSlaveRequest body) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        SlaveNode node = masterService.registerSlave(tenantId, body.getSlaveAddress(), body.getSlaveTenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(node);
    }

    @DeleteMapping("/{slaveAddress}")
    public ResponseEntity<Void> deregisterSlave(
            HttpServletRequest request,
            @PathVariable String slaveAddress) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        masterService.deregisterSlave(tenantId, slaveAddress);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(
            HttpServletRequest request,
            @Valid @RequestBody HeartbeatRequest body) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        masterService.heartbeat(tenantId, body.getSlaveAddress());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<SlaveNode>> listSlaves(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(masterService.listSlaves(tenantId, pageable));
    }

    @PostMapping("/push-config")
    public ResponseEntity<Map<String, Long>> pushConfig(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Page<SlaveNode> allSlaves = masterService.listSlaves(tenantId, PageRequest.of(0, Integer.MAX_VALUE));
        long onlineCount = allSlaves.getContent().stream()
                .filter(s -> s.getStatus() == SlaveStatus.ONLINE)
                .count();
        masterService.pushConfigToAllSlaves(tenantId);
        return ResponseEntity.ok(Map.of("pushed", onlineCount));
    }

    @Data
    public static class RegisterSlaveRequest {
        @NotBlank private String slaveAddress;
        @NotBlank private String slaveTenantId;
    }

    @Data
    public static class HeartbeatRequest {
        @NotBlank private String slaveAddress;
    }
}
