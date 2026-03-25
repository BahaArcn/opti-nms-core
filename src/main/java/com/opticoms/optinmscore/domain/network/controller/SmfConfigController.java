package com.opticoms.optinmscore.domain.network.controller;

import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import com.opticoms.optinmscore.domain.network.service.SmfConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.opticoms.optinmscore.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/network/config/smf")
@Tag(name = "Network - SMF Configuration")
@RequiredArgsConstructor
public class SmfConfigController {

    private final SmfConfigService smfConfigService;

    @Operation(summary = "Get SMF configuration for the tenant")
    @GetMapping
    public ResponseEntity<SmfConfig> getSmfConfig(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(smfConfigService.getSmfConfig(tenantId));
    }

    @Operation(summary = "Create or update SMF configuration")
    @PutMapping
    public ResponseEntity<SmfConfig> saveOrUpdateSmfConfig(
            HttpServletRequest request,
            @Valid @RequestBody SmfConfig config) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(smfConfigService.saveOrUpdateSmfConfig(tenantId, config));
    }
}
