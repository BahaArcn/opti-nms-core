package com.opticoms.optinmscore.domain.performance.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.performance.dto.PmMetricRequest;
import com.opticoms.optinmscore.domain.performance.dto.PmMetricResponse;
import com.opticoms.optinmscore.domain.performance.mapper.PmMetricMapper;
import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.service.PmService;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PmController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class PerformanceSecurityTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;

    @MockBean private PmService pmService;
    @MockBean private PmMetricMapper pmMetricMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void noAuth_GET_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/performance/current").param("metric", "cpu"))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAuth_POST_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/performance/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metricName\":\"cpu\",\"value\":50.0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_GET_returns200() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);
        when(pmService.getCurrentValue(eq(TENANT), eq("cpu"))).thenReturn(0.0);

        mockMvc.perform(get("/api/v1/performance/current")
                        .header("Authorization", "Bearer viewer-token")
                        .param("metric", "cpu"))
                .andExpect(status().isOk());
    }

    @Test
    void viewerRole_POST_returns403() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);

        mockMvc.perform(post("/api/v1/performance/metrics")
                        .header("Authorization", "Bearer viewer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metricName\":\"cpu\",\"value\":50.0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorRole_POST_returns200() throws Exception {
        User operator = buildUser("operator", User.Role.OPERATOR);
        stubAuthentication("op-token", operator);

        PmMetric metric = new PmMetric();
        when(pmMetricMapper.toEntity(any(PmMetricRequest.class))).thenReturn(metric);
        when(pmService.ingestMetric(eq(TENANT), any(PmMetric.class))).thenReturn(metric);
        when(pmMetricMapper.toResponse(any(PmMetric.class))).thenReturn(new PmMetricResponse());

        mockMvc.perform(post("/api/v1/performance/metrics")
                        .header("Authorization", "Bearer op-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metricName\":\"cpu\",\"value\":50.0}"))
                .andExpect(status().isCreated());
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
