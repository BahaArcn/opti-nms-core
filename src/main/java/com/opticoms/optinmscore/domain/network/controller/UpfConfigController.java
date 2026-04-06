package com.opticoms.optinmscore.domain.network.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.network.dto.UpfConfigRequest;
import com.opticoms.optinmscore.domain.network.dto.UpfConfigResponse;
import com.opticoms.optinmscore.domain.network.mapper.NetworkConfigMapper;
import com.opticoms.optinmscore.domain.network.service.UpfConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/network/config/upf")
@Tag(name = "Network - UPF Configuration")
@RequiredArgsConstructor
public class UpfConfigController {

    private final UpfConfigService upfConfigService;
    private final NetworkConfigMapper networkConfigMapper;

    @Operation(summary = "Get UPF configuration for the tenant")
    @GetMapping
    public ResponseEntity<UpfConfigResponse> getUpfConfig(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkConfigMapper.toUpfConfigResponse(upfConfigService.getUpfConfig(tenantId)));
    }

    @Operation(summary = "Create or update UPF configuration")
    @PutMapping
    public ResponseEntity<UpfConfigResponse> saveOrUpdateUpfConfig(
            HttpServletRequest request,
            @Valid @RequestBody UpfConfigRequest config) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkConfigMapper.toUpfConfigResponse(
                upfConfigService.saveOrUpdateUpfConfig(tenantId, networkConfigMapper.toUpfConfigEntity(config))));
    }
}
