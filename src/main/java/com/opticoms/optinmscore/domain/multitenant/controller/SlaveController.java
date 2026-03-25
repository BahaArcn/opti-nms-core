package com.opticoms.optinmscore.domain.multitenant.controller;

import com.opticoms.optinmscore.domain.multitenant.model.MultiTenantConfigPayload;
import com.opticoms.optinmscore.domain.multitenant.service.SlaveClientService;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/slave")
@Tag(name = "Slave - Config Receiver")
@RequiredArgsConstructor
public class SlaveController {

    private final SlaveClientService slaveClientService;
    private final GlobalConfigRepository globalConfigRepository;

    @PostMapping("/config")
    public ResponseEntity<Void> applyConfig(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody MultiTenantConfigPayload payload) {
        slaveClientService.applyConfigFromMaster(tenantId, payload);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public ResponseEntity<SlaveStatusResponse> getStatus(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        GlobalConfig config = globalConfigRepository.findByTenantId(tenantId).orElse(null);

        SlaveStatusResponse response = new SlaveStatusResponse();
        response.setWorkAsMaster(config != null && config.isWorkAsMaster());
        response.setMasterAddr(config != null ? config.getMasterAddr() : null);
        response.setSelfAddress(slaveClientService.getSelfAddress());
        response.setTenantId(tenantId);

        return ResponseEntity.ok(response);
    }

    @Data
    public static class SlaveStatusResponse {
        private boolean workAsMaster;
        private String masterAddr;
        private String selfAddress;
        private String tenantId;
    }
}
