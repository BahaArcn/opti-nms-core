package com.opticoms.optinmscore.domain.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.UserService;
import com.opticoms.optinmscore.security.JwtService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_returns201() throws Exception {
        User user = buildUser("newuser");
        when(userService.createUser(eq(TENANT), eq("newuser"), eq("new@example.com"),
                eq("password123"), eq(User.Role.OPERATOR))).thenReturn(user);

        String body = """
                {
                    "username": "newuser",
                    "email": "new@example.com",
                    "password": "password123",
                    "role": "OPERATOR"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_returns200() throws Exception {
        when(userService.listUsers(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildUser("user1"))));

        mockMvc.perform(get("/api/v1/users")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("user1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUser_returns200() throws Exception {
        User user = buildUser("testuser");
        when(userService.getUserById(TENANT, "user-1")).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/user-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/users/user-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(TENANT, "user-1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRole_returns200() throws Exception {
        User user = buildUser("testuser");
        user.setRole(User.Role.ADMIN);
        when(userService.updateUserRole(TENANT, "user-1", User.Role.ADMIN)).thenReturn(user);

        mockMvc.perform(put("/api/v1/users/user-1/role")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleActive_returns200() throws Exception {
        User user = buildUser("testuser");
        user.setActive(false);
        when(userService.toggleUserActive(TENANT, "user-1", false)).thenReturn(user);

        mockMvc.perform(put("/api/v1/users/user-1/status")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changePassword_returns200() throws Exception {
        mockMvc.perform(put("/api/v1/users/user-1/password")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"oldPass1\", \"newPassword\": \"newPass1\"}"))
                .andExpect(status().isOk());

        verify(userService).changePassword(TENANT, "user-1", "oldPass1", "newPass1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resetPassword_returns200() throws Exception {
        mockMvc.perform(put("/api/v1/users/user-1/reset-password")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\": \"resetPw1\"}"))
                .andExpect(status().isOk());

        verify(userService).resetPassword(TENANT, "user-1", "resetPw1");
    }

    private User buildUser(String username) {
        User u = new User();
        u.setId("user-1");
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setRole(User.Role.OPERATOR);
        u.setTenantId(TENANT);
        u.setActive(true);
        return u;
    }
}
