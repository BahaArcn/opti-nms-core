package com.opticoms.optinmscore.domain.network.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.network.dto.GlobalConfigRequest;
import com.opticoms.optinmscore.domain.network.dto.GlobalConfigResponse;
import com.opticoms.optinmscore.domain.network.mapper.NetworkConfigMapper;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.service.NetworkConfigService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NetworkConfigController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class NetworkSecurityTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE = "/api/v1/network/config";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NetworkConfigService networkConfigService;
    @MockBean private NetworkConfigMapper networkConfigMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void noAuth_GET_returns403() throws Exception {
        mockMvc.perform(get(BASE + "/global"))
                .andExpect(status().isForbidden());
    }

    @Test
    void noAuth_PUT_returns403() throws Exception {
        mockMvc.perform(put(BASE + "/global")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_GET_returns200() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);
        when(networkConfigService.getGlobalConfig(any())).thenReturn(new GlobalConfig());
        when(networkConfigMapper.toGlobalConfigResponse(any(GlobalConfig.class)))
                .thenReturn(new GlobalConfigResponse());

        mockMvc.perform(get(BASE + "/global")
                        .header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isOk());
    }

    @Test
    void viewerRole_PUT_returns403() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);

        mockMvc.perform(put(BASE + "/global")
                        .header("Authorization", "Bearer viewer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorRole_PUT_returns403() throws Exception {
        User operator = buildUser("operator", User.Role.OPERATOR);
        stubAuthentication("op-token", operator);

        mockMvc.perform(put(BASE + "/global")
                        .header("Authorization", "Bearer op-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_PUT_returns200() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN);
        stubAuthentication("admin-token", admin);

        GlobalConfigRequest body = new GlobalConfigRequest();
        body.setNetworkFullName("N");
        body.setNetworkShortName("NS");
        body.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);
        body.setAuthMethod(GlobalConfig.AuthMethod.FIVE_G_AKA);

        when(networkConfigMapper.toGlobalConfigEntity(any(GlobalConfigRequest.class)))
                .thenAnswer(inv -> new GlobalConfig());
        when(networkConfigService.saveOrUpdateGlobalConfig(eq(TENANT), any(GlobalConfig.class)))
                .thenAnswer(inv -> {
                    GlobalConfig saved = inv.getArgument(1);
                    saved.setId("gc-1");
                    saved.setTenantId(TENANT);
                    return saved;
                });
        when(networkConfigMapper.toGlobalConfigResponse(any(GlobalConfig.class)))
                .thenReturn(new GlobalConfigResponse());

        mockMvc.perform(put(BASE + "/global")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
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
