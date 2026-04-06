package com.opticoms.optinmscore.domain.dashboard.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.dashboard.service.DashboardService;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class DashboardSecurityTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;

    @MockBean private DashboardService dashboardService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void noAuth_GET_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_GET_returns200() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);
        when(dashboardService.getDashboardSummary(eq(TENANT)))
                .thenReturn(DashboardService.DashboardSummary.builder()
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
                        .build());

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isOk());
    }

    @Test
    void operatorRole_GET_returns200() throws Exception {
        User operator = buildUser("operator", User.Role.OPERATOR);
        stubAuthentication("op-token", operator);
        when(dashboardService.getDashboardSummary(eq(TENANT)))
                .thenReturn(DashboardService.DashboardSummary.builder()
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
                        .build());

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer op-token"))
                .andExpect(status().isOk());
    }

    @Test
    void adminRole_GET_returns200() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN);
        stubAuthentication("admin-token", admin);
        when(dashboardService.getDashboardSummary(eq(TENANT)))
                .thenReturn(DashboardService.DashboardSummary.builder()
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
                        .build());

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    private void stubAuthentication(String token, User user) {
        when(jwtService.extractUsername(token)).thenReturn(user.getUsername());
        when(jwtService.extractTenantId(token)).thenReturn(TENANT);
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(true);
        when(customUserDetailsService.loadUserByUsernameAndTenantId(
                user.getUsername(), TENANT)).thenReturn(user);
    }

    private User buildUser(String username, User.Role role) {
        User u = new User();
        u.setId(username + "-id");
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword("encoded-password");
        u.setRole(role);
        u.setTenantId(TENANT);
        u.setActive(true);
        return u;
    }
}
