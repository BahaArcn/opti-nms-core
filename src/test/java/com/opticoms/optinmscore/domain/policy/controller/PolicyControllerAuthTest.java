package com.opticoms.optinmscore.domain.policy.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.policy.mapper.PolicyMapper;
import com.opticoms.optinmscore.domain.policy.service.PolicyService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, MasterTokenFilterConfig.class, RateLimitTestConfig.class})
@TestPropertySource(properties = {
        "app.master-token=test-master-token",
        "app.security.master-key=test-key-minimum-32-characters!!",
        "app.security.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "app.security.jwt.expiration-ms=86400000",
        "app.cors.allowed-origins=*"
})
class PolicyControllerAuthTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;

    @MockBean PolicyService policyService;
    @MockBean PolicyMapper policyMapper;

    @Test
    void postPolicy_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/policies")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void postPolicy_viewerRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/policies")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void postPolicy_operatorRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/policies")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postPolicy_adminRole_notBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/policies")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .requestAttr("tenantId", "TEST-0001/0001/01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getPolicy_operatorRole_notBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/policies")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .requestAttr("tenantId", "TEST-0001/0001/01"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getPolicy_viewerRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/policies")
                        .header("X-Tenant-ID", "TEST-0001/0001/01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePolicy_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/policies/some-id")
                        .header("X-Tenant-ID", "TEST-0001/0001/01"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePolicy_adminRole_notBlocked() throws Exception {
        mockMvc.perform(delete("/api/v1/policies/some-id")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .requestAttr("tenantId", "TEST-0001/0001/01"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }
}
