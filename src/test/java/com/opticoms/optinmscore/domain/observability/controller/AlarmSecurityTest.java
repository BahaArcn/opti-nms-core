package com.opticoms.optinmscore.domain.observability.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.observability.dto.AlarmRequest;
import com.opticoms.optinmscore.domain.observability.dto.AlarmResponse;
import com.opticoms.optinmscore.domain.observability.mapper.AlarmMapper;
import com.opticoms.optinmscore.domain.observability.model.Alarm;
import com.opticoms.optinmscore.domain.observability.service.AlarmService;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlarmController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class AlarmSecurityTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;

    @MockBean private AlarmService alarmService;
    @MockBean private AlarmMapper alarmMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void noAuth_GET_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/fault/alarms"))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAuth_POST_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/fault/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"test\",\"alarmType\":\"test\",\"severity\":\"MAJOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_GET_returns200() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);
        when(alarmService.getAlarms(any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/fault/alarms")
                        .header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isOk());
    }

    @Test
    void viewerRole_POST_returns403() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);

        mockMvc.perform(post("/api/v1/fault/alarms")
                        .header("Authorization", "Bearer viewer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"test\",\"alarmType\":\"test\",\"severity\":\"MAJOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorRole_POST_returns200() throws Exception {
        User operator = buildUser("operator", User.Role.OPERATOR);
        stubAuthentication("op-token", operator);

        Alarm alarm = new Alarm();
        when(alarmMapper.toEntity(any(AlarmRequest.class))).thenReturn(alarm);
        when(alarmService.raiseAlarm(eq(TENANT), any(Alarm.class)))
                .thenReturn(new AlarmService.RaiseResult(alarm, false));
        when(alarmMapper.toResponse(any(Alarm.class))).thenReturn(new AlarmResponse());

        mockMvc.perform(post("/api/v1/fault/alarms")
                        .header("Authorization", "Bearer op-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"test\",\"alarmType\":\"test\",\"severity\":\"MAJOR\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminRole_GET_returns200() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN);
        stubAuthentication("admin-token", admin);
        when(alarmService.getAlarms(any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/fault/alarms")
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
