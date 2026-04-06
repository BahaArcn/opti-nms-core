package com.opticoms.optinmscore.integration.open5gs.deploy.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.integration.open5gs.deploy.dto.DeployResult;
import com.opticoms.optinmscore.integration.open5gs.deploy.dto.RenderedConfigs;
import com.opticoms.optinmscore.integration.open5gs.deploy.service.ConfigRenderService;
import com.opticoms.optinmscore.integration.open5gs.deploy.service.KubernetesDeployService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Open5gsDeployController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class Open5gsDeploySecurityTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE = "/api/v1/open5gs/deploy";

    @Autowired private MockMvc mockMvc;
    @MockBean private ConfigRenderService configRenderService;
    @MockBean private KubernetesDeployService kubernetesDeployService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void noAuth_GET_403() throws Exception {
        mockMvc.perform(get(BASE + "/preview")).andExpect(status().isForbidden());
    }

    @Test
    void noAuth_POST_403() throws Exception {
        mockMvc.perform(post(BASE + "/all")).andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_GET_403() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);

        mockMvc.perform(get(BASE + "/preview").header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorRole_POST_403() throws Exception {
        User operator = buildUser("operator", User.Role.OPERATOR);
        stubAuthentication("op-token", operator);

        mockMvc.perform(post(BASE + "/all").header("Authorization", "Bearer op-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_GET_200() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN);
        stubAuthentication("admin-token", admin);
        RenderedConfigs rendered = RenderedConfigs.builder()
                .amfYaml("")
                .smfYaml("")
                .upfYaml("")
                .wrapperSh("")
                .nrfYaml("")
                .nssfYaml("")
                .build();
        when(configRenderService.renderAll(eq(TENANT))).thenReturn(rendered);

        mockMvc.perform(get(BASE + "/preview").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void adminRole_POST_200() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN);
        stubAuthentication("admin-token", admin);
        RenderedConfigs rendered = RenderedConfigs.builder().amfYaml("x").build();
        when(configRenderService.renderAll(eq(TENANT))).thenReturn(rendered);
        when(kubernetesDeployService.applyAll(rendered)).thenReturn(
                DeployResult.builder()
                        .success(true)
                        .deployedAt(Instant.parse("2026-04-06T12:00:00Z"))
                        .updatedConfigMaps(List.of())
                        .restartedDeployments(List.of())
                        .errors(List.of())
                        .successCount(0)
                        .failureCount(0)
                        .dryRun(false)
                        .build());

        mockMvc.perform(post(BASE + "/all").header("Authorization", "Bearer admin-token"))
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
