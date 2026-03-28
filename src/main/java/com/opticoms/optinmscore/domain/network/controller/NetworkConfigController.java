package com.opticoms.optinmscore.domain.network.controller;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.opticoms.optinmscore.domain.network.service.NetworkConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.opticoms.optinmscore.common.util.TenantContext;
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

    /**
     * Endpoint 1: Mevcut konfigürasyonu getir (GET)
     */
    @Operation(summary = "Get global network configuration")
    @GetMapping("/global")
    public ResponseEntity<GlobalConfig> getGlobalConfig(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);

        GlobalConfig config = networkConfigService.getGlobalConfig(tenantId);

        // 200 OK HTTP statüsü ile veriyi JSON olarak dönüyoruz.
        return ResponseEntity.ok(config);
    }

    /**
     * Endpoint 2: Yeni konfigürasyon kaydet veya olanı güncelle (PUT)
     */
    @Operation(summary = "Update global network configuration")
    @PutMapping("/global")
    public ResponseEntity<GlobalConfig> saveOrUpdateGlobalConfig(
            HttpServletRequest request,
            @Valid @RequestBody GlobalConfig newConfig) {
        String tenantId = TenantContext.getCurrentTenantId(request);

        GlobalConfig savedConfig = networkConfigService.saveOrUpdateGlobalConfig(tenantId, newConfig);

        return ResponseEntity.ok(savedConfig);
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