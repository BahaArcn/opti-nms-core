package com.opticoms.optinmscore.domain.network.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.network.dto.AmfConfigRequest;
import com.opticoms.optinmscore.domain.network.dto.AmfConfigResponse;
import com.opticoms.optinmscore.domain.network.mapper.NetworkConfigMapper;
import com.opticoms.optinmscore.domain.network.service.AmfConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/network/config/amf")
@Tag(name = "Network - AMF Configuration")
@RequiredArgsConstructor
public class AmfConfigController {

    private final AmfConfigService amfConfigService;
    private final NetworkConfigMapper networkConfigMapper;

    @Operation(summary = "Get AMF configuration for the tenant")
    @GetMapping
    public ResponseEntity<AmfConfigResponse> getAmfConfig(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkConfigMapper.toAmfConfigResponse(amfConfigService.getAmfConfig(tenantId)));
    }

    @Operation(summary = "Create or update AMF configuration")
    @PutMapping
    public ResponseEntity<AmfConfigResponse> saveOrUpdateAmfConfig(
            HttpServletRequest request,
            @Valid @RequestBody AmfConfigRequest config) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkConfigMapper.toAmfConfigResponse(
                amfConfigService.saveOrUpdateAmfConfig(tenantId, networkConfigMapper.toAmfConfigEntity(config))));
    }
}
