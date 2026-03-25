package com.opticoms.optinmscore.domain.network.controller;

import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import com.opticoms.optinmscore.domain.network.service.AmfConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.opticoms.optinmscore.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/network/config/amf")
@Tag(name = "Network - AMF Configuration")
@RequiredArgsConstructor
public class AmfConfigController {

    private final AmfConfigService amfConfigService;

    @Operation(summary = "Get AMF configuration for the tenant")
    @GetMapping
    public ResponseEntity<AmfConfig> getAmfConfig(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(amfConfigService.getAmfConfig(tenantId));
    }

    @Operation(summary = "Create or update AMF configuration")
    @PutMapping
    public ResponseEntity<AmfConfig> saveOrUpdateAmfConfig(
            HttpServletRequest request,
            @Valid @RequestBody AmfConfig config) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(amfConfigService.saveOrUpdateAmfConfig(tenantId, config));
    }
}
