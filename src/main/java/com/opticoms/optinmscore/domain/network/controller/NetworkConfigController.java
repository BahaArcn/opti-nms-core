package com.opticoms.optinmscore.domain.network.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.network.dto.GlobalConfigRequest;
import com.opticoms.optinmscore.domain.network.dto.GlobalConfigResponse;
import com.opticoms.optinmscore.domain.network.mapper.NetworkConfigMapper;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.service.NetworkConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/network/config")
@Tag(name = "Network - Global Configuration")
@RequiredArgsConstructor
public class NetworkConfigController {

    private final NetworkConfigService networkConfigService;
    private final NetworkConfigMapper networkConfigMapper;

    @Operation(summary = "Get global network configuration")
    @GetMapping("/global")
    public ResponseEntity<GlobalConfigResponse> getGlobalConfig(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);

        GlobalConfig config = networkConfigService.getGlobalConfig(tenantId);

        return ResponseEntity.ok(networkConfigMapper.toGlobalConfigResponse(config));
    }

    @Operation(summary = "Update global network configuration")
    @PutMapping("/global")
    public ResponseEntity<GlobalConfigResponse> saveOrUpdateGlobalConfig(
            HttpServletRequest request,
            @Valid @RequestBody GlobalConfigRequest newConfig) {
        String tenantId = TenantContext.getCurrentTenantId(request);

        GlobalConfig savedConfig = networkConfigService.saveOrUpdateGlobalConfig(
                tenantId, networkConfigMapper.toGlobalConfigEntity(newConfig));

        return ResponseEntity.ok(networkConfigMapper.toGlobalConfigResponse(savedConfig));
    }

    @Operation(summary = "Get UE IP pool list for the tenant (for DNN combo-box population)")
    @GetMapping("/global/ip-pools")
    public ResponseEntity<List<GlobalConfig.UeIpPool>> getIpPools(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        GlobalConfig config = networkConfigService.getGlobalConfig(tenantId);
        List<GlobalConfig.UeIpPool> pools = config.getUeIpPoolList();
        return ResponseEntity.ok(pools != null ? pools : Collections.emptyList());
    }
}
