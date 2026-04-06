package com.opticoms.optinmscore.domain.network.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.network.dto.SmfConfigRequest;
import com.opticoms.optinmscore.domain.network.dto.SmfConfigResponse;
import com.opticoms.optinmscore.domain.network.mapper.NetworkConfigMapper;
import com.opticoms.optinmscore.domain.network.service.SmfConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/network/config/smf")
@Tag(name = "Network - SMF Configuration")
@RequiredArgsConstructor
public class SmfConfigController {

    private final SmfConfigService smfConfigService;
    private final NetworkConfigMapper networkConfigMapper;

    @Operation(summary = "Get SMF configuration for the tenant")
    @GetMapping
    public ResponseEntity<SmfConfigResponse> getSmfConfig(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkConfigMapper.toSmfConfigResponse(smfConfigService.getSmfConfig(tenantId)));
    }

    @Operation(summary = "Create or update SMF configuration")
    @PutMapping
    public ResponseEntity<SmfConfigResponse> saveOrUpdateSmfConfig(
            HttpServletRequest request,
            @Valid @RequestBody SmfConfigRequest config) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(networkConfigMapper.toSmfConfigResponse(
                smfConfigService.saveOrUpdateSmfConfig(tenantId, networkConfigMapper.toSmfConfigEntity(config))));
    }
}
