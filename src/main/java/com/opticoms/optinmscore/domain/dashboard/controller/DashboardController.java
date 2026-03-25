package com.opticoms.optinmscore.domain.dashboard.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get aggregated dashboard summary with system status")
    @GetMapping("/summary")
    public ResponseEntity<DashboardService.DashboardSummary> getDashboardSummary(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(dashboardService.getDashboardSummary(tenantId));
    }
}
