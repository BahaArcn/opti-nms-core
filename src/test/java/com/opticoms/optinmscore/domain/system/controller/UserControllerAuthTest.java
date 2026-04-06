package com.opticoms.optinmscore.domain.system.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.system.mapper.UserMapper;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.domain.system.service.UserService;
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

@WebMvcTest(UserController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, MasterTokenFilterConfig.class, RateLimitTestConfig.class})
@TestPropertySource(properties = {
        "app.master-token=test-master-token",
        "app.security.master-key=test-key-minimum-32-characters!!",
        "app.security.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "app.security.jwt.expiration-ms=86400000",
        "app.cors.allowed-origins=*"
})
class UserControllerAuthTest {

    private static final String TENANT = "TEST-0001/0001/01";

    @Autowired MockMvc mockMvc;

    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean UserMapper userMapper;

    @MockBean UserService userService;

    @Test
    void createUser_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"password123\",\"role\":\"OPERATOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createUser_viewerRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"password123\",\"role\":\"OPERATOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_adminRole_notBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("X-Tenant-ID", TENANT)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"password123\",\"role\":\"OPERATOR\"}"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void changeRole_viewerRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/users/user123/role")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"OPERATOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void changeRole_operatorRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/users/user123/role")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeRole_adminRole_notBlocked() throws Exception {
        mockMvc.perform(put("/api/v1/users/user123/role")
                        .header("X-Tenant-ID", TENANT)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"OPERATOR\"}"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void changeOwnPassword_authenticatedUser_notBlocked() throws Exception {
        mockMvc.perform(put("/api/v1/users/user123/password")
                        .header("X-Tenant-ID", TENANT)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old12345\",\"newPassword\":\"new12345\"}"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void changePassword_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/users/user123/password")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old12345\",\"newPassword\":\"new12345\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getUsers_viewerRole_notBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("X-Tenant-ID", TENANT)
                        .requestAttr("tenantId", TENANT))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void deleteUser_noAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/users/user123")
                        .header("X-Tenant-ID", TENANT))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void deleteUser_operatorRole_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/users/user123")
                        .header("X-Tenant-ID", TENANT))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_adminRole_notBlocked() throws Exception {
        mockMvc.perform(delete("/api/v1/users/user123")
                        .header("X-Tenant-ID", TENANT)
                        .requestAttr("tenantId", TENANT))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }
}
