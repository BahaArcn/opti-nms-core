package com.opticoms.optinmscore.domain.networkservice.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse;
import com.opticoms.optinmscore.domain.networkservice.service.NetworkOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/networks")
@RequiredArgsConstructor
@Tag(name = "Network Services")
public class NetworkOverviewController {

    private final NetworkOverviewService networkOverviewService;

    @Operation(summary = "Get network services status derived from tenant configuration")
    @GetMapping("/overview")
    public ResponseEntity<NetworkOverviewResponse> getOverview(
            @Parameter(description = "Tenant ID", required = true, example = "TKCL-0001/0001/01")
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(networkOverviewService.getOverview(tenantId));
    }
}
