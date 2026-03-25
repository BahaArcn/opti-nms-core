package com.opticoms.optinmscore.domain.system.controller;

import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;

    @MockBean private CustomUserDetailsService userDetailsService;
    @MockBean private JwtService jwtService;
    @MockBean private PasswordEncoder passwordEncoder;

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        User user = buildUser("admin", true);
        when(userDetailsService.loadUserByUsernameAndTenantId("admin", TENANT)).thenReturn(user);
        when(passwordEncoder.matches("secret123", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token-value");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","username":"admin","password":"secret123"}
                                """.formatted(TENANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-value"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.tenantId").value(TENANT))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        User user = buildUser("admin", true);
        when(userDetailsService.loadUserByUsernameAndTenantId("admin", TENANT)).thenReturn(user);
        when(passwordEncoder.matches("wrongpass", "encoded-password")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","username":"admin","password":"wrongpass"}
                                """.formatted(TENANT)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownTenant_returns401() throws Exception {
        when(userDetailsService.loadUserByUsernameAndTenantId("admin", "UNKN-9999/9999/99"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"UNKN-9999/9999/99","username":"admin","password":"secret123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_disabledUser_returns401() throws Exception {
        User user = buildUser("admin", false);
        when(userDetailsService.loadUserByUsernameAndTenantId("admin", TENANT)).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","username":"admin","password":"secret123"}
                                """.formatted(TENANT)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingTenantId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"secret123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private User buildUser(String username, boolean active) {
        User user = new User();
        user.setId(username + "-id");
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setEmail(username + "@example.com");
        user.setRole(User.Role.ADMIN);
        user.setTenantId(TENANT);
        user.setActive(active);
        return user;
    }
}
