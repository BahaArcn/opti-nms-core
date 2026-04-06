package com.opticoms.optinmscore.domain.dashboard.controller;

import com.opticoms.optinmscore.domain.dashboard.service.DashboardService;
import com.opticoms.optinmscore.security.JwtService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;

    @MockBean private DashboardService dashboardService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VIEWER")
    void getSummary_healthy_returnsAllFields() throws Exception {
        DashboardService.DashboardSummary summary = DashboardService.DashboardSummary.builder()
                .totalSubscribers(100)
                .totalGNodeBs(5)
                .connectedGNodeBs(3)
                .disconnectedGNodeBs(2)
                .connectedUes(10)
                .idleUes(3)
                .totalUes(15)
                .activeSessions(8)
                .activeAlarms(0)
                .criticalAlarms(0)
                .majorAlarms(0)
                .systemStatus(DashboardService.SystemStatus.HEALTHY)
                .amfReachable(true)
                .smfReachable(true)
                .upfReachable(true)
                .licenseActive(true)
                .build();

        when(dashboardService.getDashboardSummary(TENANT)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubscribers").value(100))
                .andExpect(jsonPath("$.totalGNodeBs").value(5))
                .andExpect(jsonPath("$.connectedGNodeBs").value(3))
                .andExpect(jsonPath("$.disconnectedGNodeBs").value(2))
                .andExpect(jsonPath("$.connectedUes").value(10))
                .andExpect(jsonPath("$.idleUes").value(3))
                .andExpect(jsonPath("$.totalUes").value(15))
                .andExpect(jsonPath("$.activeSessions").value(8))
                .andExpect(jsonPath("$.activeAlarms").value(0))
                .andExpect(jsonPath("$.criticalAlarms").value(0))
                .andExpect(jsonPath("$.majorAlarms").value(0))
                .andExpect(jsonPath("$.systemStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.amfReachable").value(true))
                .andExpect(jsonPath("$.smfReachable").value(true))
                .andExpect(jsonPath("$.upfReachable").value(true))
                .andExpect(jsonPath("$.licenseActive").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSummary_degraded_whenMajorAlarms() throws Exception {
        DashboardService.DashboardSummary summary = DashboardService.DashboardSummary.builder()
                .totalSubscribers(50)
                .totalGNodeBs(3)
                .connectedGNodeBs(3)
                .disconnectedGNodeBs(0)
                .connectedUes(5)
                .idleUes(0)
                .totalUes(5)
                .activeSessions(5)
                .activeAlarms(2)
                .criticalAlarms(0)
                .majorAlarms(2)
                .systemStatus(DashboardService.SystemStatus.DEGRADED)
                .amfReachable(true)
                .smfReachable(true)
                .upfReachable(false)
                .licenseActive(true)
                .build();

        when(dashboardService.getDashboardSummary(TENANT)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus").value("DEGRADED"))
                .andExpect(jsonPath("$.majorAlarms").value(2))
                .andExpect(jsonPath("$.activeAlarms").value(2))
                .andExpect(jsonPath("$.upfReachable").value(false));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getSummary_down_whenCriticalAlarms() throws Exception {
        DashboardService.DashboardSummary summary = DashboardService.DashboardSummary.builder()
                .totalSubscribers(0)
                .totalGNodeBs(0)
                .connectedGNodeBs(0)
                .disconnectedGNodeBs(0)
                .connectedUes(0)
                .idleUes(0)
                .totalUes(0)
                .activeSessions(0)
                .activeAlarms(3)
                .criticalAlarms(3)
                .majorAlarms(0)
                .systemStatus(DashboardService.SystemStatus.DOWN)
                .amfReachable(false)
                .smfReachable(false)
                .upfReachable(false)
                .licenseActive(false)
                .build();

        when(dashboardService.getDashboardSummary(TENANT)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus").value("DOWN"))
                .andExpect(jsonPath("$.criticalAlarms").value(3))
                .andExpect(jsonPath("$.amfReachable").value(false))
                .andExpect(jsonPath("$.smfReachable").value(false))
                .andExpect(jsonPath("$.licenseActive").value(false))
                .andExpect(jsonPath("$.totalSubscribers").value(0));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getSummary_emptyTenant_returnsZeroCounts() throws Exception {
        DashboardService.DashboardSummary summary = DashboardService.DashboardSummary.builder()
                .totalSubscribers(0)
                .totalGNodeBs(0)
                .connectedGNodeBs(0)
                .disconnectedGNodeBs(0)
                .connectedUes(0)
                .idleUes(0)
                .totalUes(0)
                .activeSessions(0)
                .activeAlarms(0)
                .criticalAlarms(0)
                .majorAlarms(0)
                .systemStatus(DashboardService.SystemStatus.HEALTHY)
                .amfReachable(true)
                .smfReachable(true)
                .upfReachable(true)
                .licenseActive(true)
                .build();

        when(dashboardService.getDashboardSummary(TENANT)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubscribers").value(0))
                .andExpect(jsonPath("$.totalGNodeBs").value(0))
                .andExpect(jsonPath("$.connectedUes").value(0))
                .andExpect(jsonPath("$.activeSessions").value(0))
                .andExpect(jsonPath("$.systemStatus").value("HEALTHY"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSummary_serviceThrowsException_returns500() throws Exception {
        when(dashboardService.getDashboardSummary(TENANT))
                .thenThrow(new RuntimeException("Database unreachable"));

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isInternalServerError());
    }
}
