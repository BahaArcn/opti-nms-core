package com.opticoms.optinmscore.domain.tenant.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.domain.system.service.UserService;
import com.opticoms.optinmscore.domain.tenant.mapper.TenantMapper;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
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

@WebMvcTest(TenantController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, MasterTokenFilterConfig.class, RateLimitTestConfig.class})
@TestPropertySource(properties = {
        "app.master-token=test-master-token",
        "app.security.master-key=test-key-minimum-32-characters!!",
        "app.security.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "app.security.jwt.expiration-ms=86400000",
        "app.cors.allowed-origins=*"
})
class TenantControllerAuthTest {

    @Autowired MockMvc mockMvc;

    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;

    @MockBean TenantService tenantService;
    @MockBean UserService userService;
    @MockBean TenantMapper tenantMapper;

    @Test
    void onboardTenant_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/system/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"NEW-0001/0001/01\",\"companyName\":\"Test\",\"adminUsername\":\"admin\",\"adminPassword\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void onboardTenant_adminRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/system/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"NEW-0001/0001/01\",\"companyName\":\"Test\",\"adminUsername\":\"admin\",\"adminPassword\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void onboardTenant_operatorRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/system/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"NEW-0001/0001/01\",\"companyName\":\"Test\",\"adminUsername\":\"admin\",\"adminPassword\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void onboardTenant_superAdminRole_notBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/system/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"NEW-0001/0001/01\",\"companyName\":\"Test\",\"adminUsername\":\"admin\",\"adminPassword\":\"password123\"}"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getTenants_superAdminRole_notBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/system/tenants"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTenants_adminRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/system/tenants"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTenants_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/system/tenants"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void deactivateTenant_superAdminRole_notBlocked() throws Exception {
        mockMvc.perform(put("/api/v1/system/tenants/TENANT-001/deactivate"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateTenant_adminRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/system/tenants/TENANT-001/deactivate"))
                .andExpect(status().isForbidden());
    }
}
