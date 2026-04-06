package com.opticoms.optinmscore.domain.firewall.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.firewall.mapper.FirewallRuleMapper;
import com.opticoms.optinmscore.domain.firewall.service.FirewallService;
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

@WebMvcTest(FirewallController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, MasterTokenFilterConfig.class, RateLimitTestConfig.class})
@TestPropertySource(properties = {
        "app.master-token=test-master-token",
        "app.security.master-key=test-key-minimum-32-characters!!",
        "app.security.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "app.security.jwt.expiration-ms=86400000",
        "app.cors.allowed-origins=*"
})
class FirewallControllerAuthTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;

    @MockBean FirewallService firewallService;
    @MockBean FirewallRuleMapper firewallRuleMapper;

    @Test
    void postFirewallRule_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/firewall/rules")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void postFirewallRule_operatorRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/firewall/rules")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void postFirewallRule_viewerRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/firewall/rules")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postFirewallRule_adminRole_notBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/firewall/rules")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .requestAttr("tenantId", "TEST-0001/0001/01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void getFirewallRules_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/firewall/rules")
                        .header("X-Tenant-ID", "TEST-0001/0001/01"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getFirewallRules_operatorRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/firewall/rules")
                        .header("X-Tenant-ID", "TEST-0001/0001/01"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getFirewallRules_adminRole_notBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/firewall/rules")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .requestAttr("tenantId", "TEST-0001/0001/01"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void deleteFirewallRule_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/firewall/rules/rule123")
                        .header("X-Tenant-ID", "TEST-0001/0001/01"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void deleteFirewallRule_viewerRole_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/firewall/rules/rule123")
                        .header("X-Tenant-ID", "TEST-0001/0001/01"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteFirewallRule_adminRole_notBlocked() throws Exception {
        mockMvc.perform(delete("/api/v1/firewall/rules/rule123")
                        .header("X-Tenant-ID", "TEST-0001/0001/01")
                        .requestAttr("tenantId", "TEST-0001/0001/01"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }
}
