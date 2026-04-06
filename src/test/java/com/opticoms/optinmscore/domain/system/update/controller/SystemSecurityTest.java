package com.opticoms.optinmscore.domain.system.update.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilter;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.domain.system.update.dto.VersionInfo;
import com.opticoms.optinmscore.domain.system.update.service.UpdateService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemUpdateController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class SystemSecurityTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String PLATFORM_TENANT = "PLAT-0000/0000/00";

    @Autowired private MockMvc mockMvc;

    @MockBean private UpdateService updateService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void noAuth_GET_version_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/system/version"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_GET_version_returns403() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN, TENANT);
        stubAuthentication("admin-token", admin, TENANT);

        mockMvc.perform(get("/api/v1/system/version")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_POST_updateCheck_returns403() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN, TENANT);
        stubAuthentication("admin-token", admin, TENANT);

        mockMvc.perform(post("/api/v1/system/update/check")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminRole_GET_version_returns200() throws Exception {
        User superAdmin = buildUser("superadmin", User.Role.SUPER_ADMIN, PLATFORM_TENANT);
        stubAuthentication("super-token", superAdmin, PLATFORM_TENANT);
        when(updateService.getVersion()).thenReturn(new VersionInfo());

        mockMvc.perform(get("/api/v1/system/version")
                        .header("Authorization", "Bearer super-token"))
                .andExpect(status().isOk());
    }

    private void stubAuthentication(String token, User user, String tenantId) {
        when(jwtService.extractUsername(token)).thenReturn(user.getUsername());
        when(jwtService.extractTenantId(token)).thenReturn(tenantId);
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(true);
        when(customUserDetailsService.loadUserByUsernameAndTenantId(
                user.getUsername(), tenantId)).thenReturn(user);
    }

    private User buildUser(String username, User.Role role, String tenantId) {
        User u = new User();
        u.setId(username + "-id");
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword("encoded-password");
        u.setRole(role);
        u.setTenantId(tenantId);
        u.setActive(true);
        return u;
    }
}
