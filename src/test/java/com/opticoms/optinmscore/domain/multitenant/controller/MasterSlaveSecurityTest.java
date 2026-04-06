package com.opticoms.optinmscore.domain.multitenant.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.multitenant.mapper.SlaveNodeMapper;
import com.opticoms.optinmscore.domain.multitenant.service.MasterService;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MasterController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class MasterSlaveSecurityTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String SLAVES = "/api/v1/master/slaves";

    @Autowired private MockMvc mockMvc;
    @MockBean private MasterService masterService;
    @MockBean private SlaveNodeMapper slaveNodeMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void noAuth_GET_slaves_returns403() throws Exception {
        mockMvc.perform(get(SLAVES)).andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_GET_slaves_returns403() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);

        mockMvc.perform(get(SLAVES).header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_GET_slaves_returns200() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN);
        stubAuthentication("admin-token", admin);
        when(masterService.listSlaves(eq(TENANT), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(SLAVES).header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void operatorRole_pushConfig_returns403() throws Exception {
        User operator = buildUser("operator", User.Role.OPERATOR);
        stubAuthentication("op-token", operator);

        mockMvc.perform(post(SLAVES + "/push-config").header("Authorization", "Bearer op-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_pushConfig_returns200() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN);
        stubAuthentication("admin-token", admin);
        when(masterService.listSlaves(eq(TENANT), any(Pageable.class))).thenReturn(Page.empty());
        doNothing().when(masterService).pushConfigToAllSlaves(eq(TENANT));

        mockMvc.perform(post(SLAVES + "/push-config").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    private void stubAuthentication(String token, User user) {
        when(jwtService.extractUsername(token)).thenReturn(user.getUsername());
        when(jwtService.extractTenantId(token)).thenReturn(TENANT);
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(true);
        when(customUserDetailsService.loadUserByUsernameAndTenantId(user.getUsername(), TENANT)).thenReturn(user);
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
