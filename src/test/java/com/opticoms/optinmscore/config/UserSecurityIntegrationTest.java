package com.opticoms.optinmscore.config;

import com.opticoms.optinmscore.domain.system.controller.UserController;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.domain.system.service.UserService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController RBAC tests with real security filter chain.
 * Verifies SecurityConfiguration rules for /api/v1/users/** endpoints:
 *   POST, DELETE, PUT role/status/reset-password → ADMIN only
 *   PUT password → authenticated (any role)
 *   GET → ADMIN, OPERATOR, VIEWER
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class})
class UserSecurityIntegrationTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private User adminUser;
    private User viewerUser;
    private User operatorUser;

    @BeforeEach
    void setUp() {
        adminUser = buildUser("admin", User.Role.ADMIN);
        viewerUser = buildUser("viewer", User.Role.VIEWER);
        operatorUser = buildUser("operator", User.Role.OPERATOR);
    }

    // ── POST /api/v1/users — ADMIN only ────────────────────────────────

    @Nested
    class CreateUserRbac {

        @Test
        void admin_canCreate_returns201() throws Exception {
            stubAuth("admin-token", adminUser);
            when(userService.createUser(eq(TENANT), eq("newuser"), eq("new@test.com"),
                    eq("password123"), eq(User.Role.OPERATOR))).thenReturn(buildUser("newuser", User.Role.OPERATOR));

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer admin-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"newuser","email":"new@test.com","password":"password123","role":"OPERATOR"}
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        void viewer_cannotCreate_returns403() throws Exception {
            stubAuth("viewer-token", viewerUser);

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer viewer-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"newuser","email":"new@test.com","password":"password123","role":"OPERATOR"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        void operator_cannotCreate_returns403() throws Exception {
            stubAuth("operator-token", operatorUser);

            mockMvc.perform(post("/api/v1/users")
                            .header("Authorization", "Bearer operator-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"newuser","email":"new@test.com","password":"password123","role":"OPERATOR"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    // ── DELETE /api/v1/users/{id} — ADMIN only ─────────────────────────

    @Nested
    class DeleteUserRbac {

        @Test
        void admin_canDelete_returns204() throws Exception {
            stubAuth("admin-token", adminUser);

            mockMvc.perform(delete("/api/v1/users/user-1")
                            .header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void viewer_cannotDelete_returns403() throws Exception {
            stubAuth("viewer-token", viewerUser);

            mockMvc.perform(delete("/api/v1/users/user-1")
                            .header("Authorization", "Bearer viewer-token"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PUT /api/v1/users/{id}/role — ADMIN only ───────────────────────

    @Nested
    class UpdateRoleRbac {

        @Test
        void admin_canUpdateRole() throws Exception {
            stubAuth("admin-token", adminUser);
            when(userService.updateUserRole(TENANT, "user-1", User.Role.ADMIN))
                    .thenReturn(buildUser("target", User.Role.ADMIN));

            mockMvc.perform(put("/api/v1/users/user-1/role")
                            .header("Authorization", "Bearer admin-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void operator_cannotUpdateRole_returns403() throws Exception {
            stubAuth("operator-token", operatorUser);

            mockMvc.perform(put("/api/v1/users/user-1/role")
                            .header("Authorization", "Bearer operator-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"role\":\"ADMIN\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PUT /api/v1/users/{id}/status — ADMIN only ─────────────────────

    @Nested
    class ToggleStatusRbac {

        @Test
        void admin_canToggleStatus() throws Exception {
            stubAuth("admin-token", adminUser);
            when(userService.toggleUserActive(TENANT, "user-1", false))
                    .thenReturn(buildUser("target", User.Role.OPERATOR));

            mockMvc.perform(put("/api/v1/users/user-1/status")
                            .header("Authorization", "Bearer admin-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"active\":false}"))
                    .andExpect(status().isOk());
        }

        @Test
        void viewer_cannotToggleStatus_returns403() throws Exception {
            stubAuth("viewer-token", viewerUser);

            mockMvc.perform(put("/api/v1/users/user-1/status")
                            .header("Authorization", "Bearer viewer-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"active\":false}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PUT /api/v1/users/{id}/password — authenticated (any role) ─────

    @Nested
    class ChangePasswordRbac {

        @Test
        void viewer_canChangeOwnPassword() throws Exception {
            stubAuth("viewer-token", viewerUser);

            mockMvc.perform(put("/api/v1/users/user-1/password")
                            .header("Authorization", "Bearer viewer-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"oldPwd123\",\"newPassword\":\"newPwd123\"}"))
                    .andExpect(status().isOk());

            verify(userService).changePassword(TENANT, "user-1", "oldPwd123", "newPwd123");
        }

        @Test
        void operator_canChangeOwnPassword() throws Exception {
            stubAuth("operator-token", operatorUser);

            mockMvc.perform(put("/api/v1/users/user-1/password")
                            .header("Authorization", "Bearer operator-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"oldPwd123\",\"newPassword\":\"newPwd123\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        void unauthenticated_cannotChangePassword_returns403() throws Exception {
            mockMvc.perform(put("/api/v1/users/user-1/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"oldPwd1\",\"newPassword\":\"newPwd1\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PUT /api/v1/users/{id}/reset-password — ADMIN only ─────────────

    @Nested
    class ResetPasswordRbac {

        @Test
        void admin_canResetPassword() throws Exception {
            stubAuth("admin-token", adminUser);

            mockMvc.perform(put("/api/v1/users/user-1/reset-password")
                            .header("Authorization", "Bearer admin-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"resetPw1\"}"))
                    .andExpect(status().isOk());

            verify(userService).resetPassword(TENANT, "user-1", "resetPw1");
        }

        @Test
        void viewer_cannotResetPassword_returns403() throws Exception {
            stubAuth("viewer-token", viewerUser);

            mockMvc.perform(put("/api/v1/users/user-1/reset-password")
                            .header("Authorization", "Bearer viewer-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newPassword\":\"resetPw1\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /api/v1/users — all roles ──────────────────────────────────

    @Nested
    class ReadUsersRbac {

        @Test
        void viewer_canListUsers() throws Exception {
            stubAuth("viewer-token", viewerUser);
            when(userService.listUsers(eq(TENANT), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(buildUser("u1", User.Role.OPERATOR))));

            mockMvc.perform(get("/api/v1/users")
                            .header("Authorization", "Bearer viewer-token"))
                    .andExpect(status().isOk());
        }

        @Test
        void operator_canGetUser() throws Exception {
            stubAuth("operator-token", operatorUser);
            when(userService.getUserById(TENANT, "user-1"))
                    .thenReturn(buildUser("target", User.Role.OPERATOR));

            mockMvc.perform(get("/api/v1/users/user-1")
                            .header("Authorization", "Bearer operator-token"))
                    .andExpect(status().isOk());
        }

        @Test
        void unauthenticated_cannotListUsers_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void stubAuth(String token, User user) {
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
