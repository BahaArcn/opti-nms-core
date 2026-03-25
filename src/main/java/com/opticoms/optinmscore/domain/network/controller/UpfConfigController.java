package com.opticoms.optinmscore.domain.network.controller;

import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import com.opticoms.optinmscore.domain.network.service.UpfConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.opticoms.optinmscore.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/network/config/upf")
@Tag(name = "Network - UPF Configuration")
@RequiredArgsConstructor
public class UpfConfigController {

    private final UpfConfigService upfConfigService;

    @Operation(summary = "Get UPF configuration for the tenant")
    @GetMapping
    public ResponseEntity<UpfConfig> getUpfConfig(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(upfConfigService.getUpfConfig(tenantId));
    }

    @Operation(summary = "Create or update UPF configuration")
    @PutMapping
    public ResponseEntity<UpfConfig> saveOrUpdateUpfConfig(
            HttpServletRequest request,
            @Valid @RequestBody UpfConfig config) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(upfConfigService.saveOrUpdateUpfConfig(tenantId, config));
    }
}
