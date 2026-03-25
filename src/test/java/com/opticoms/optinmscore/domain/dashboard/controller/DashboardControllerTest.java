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
    void getDashboardSummary_returns200() throws Exception {
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
                .build();

        when(dashboardService.getDashboardSummary(TENANT)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubscribers").value(100))
                .andExpect(jsonPath("$.systemStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.amfReachable").value(true));
    }
}
