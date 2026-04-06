package com.opticoms.optinmscore.config;

import com.opticoms.optinmscore.config.security.MasterTokenFilter;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.subscriber.controller.SubscriberController;
import com.opticoms.optinmscore.domain.subscriber.mapper.SubscriberMapper;
import com.opticoms.optinmscore.domain.subscriber.service.BulkImportService;
import com.opticoms.optinmscore.domain.subscriber.service.SubscriberService;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security integration tests with ALL filters enabled:
 * JwtAuthenticationFilter -> RBAC (SecurityConfiguration).
 *
 * Tenant identity is derived from JWT (via extractTenantId), not from X-Tenant-ID header.
 */
@WebMvcTest(SubscriberController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class SecurityIntegrationTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;

    @MockBean private SubscriberService subscriberService;
    @MockBean private BulkImportService bulkImportService;
    @MockBean private SubscriberMapper subscriberMapper;
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

    // ── Unauthenticated access: rejected by security chain ─────────────

    @Nested
    class UnauthenticatedTests {

        @Test
        void noAuthHeader_GET_returnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/subscribers/list"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void invalidBearerToken_returnsForbidden() throws Exception {
            when(jwtService.extractUsername("bad-token")).thenThrow(new RuntimeException("Invalid"));

            mockMvc.perform(get("/api/v1/subscribers/list")
                            .header("Authorization", "Bearer bad-token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void noAuthHeader_POST_returnsForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/subscribers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void noAuthHeader_DELETE_returnsForbidden() throws Exception {
            mockMvc.perform(delete("/api/v1/subscribers/286010000000001"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void authenticatedEndpoint_noToken_cannotAccessService() throws Exception {
            mockMvc.perform(get("/api/v1/subscribers/count"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(subscriberService);
        }
    }

    // ── RBAC: role enforcement ─────────────────────────────────────────

    @Nested
    class RbacTests {

        @Test
        void viewerCannotPostSubscriber_returns403() throws Exception {
            stubAuthentication("viewer-token", viewerUser);

            mockMvc.perform(post("/api/v1/subscribers")
                            .header("Authorization", "Bearer viewer-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void viewerCannotDeleteSubscriber_returns403() throws Exception {
            stubAuthentication("viewer-token", viewerUser);

            mockMvc.perform(delete("/api/v1/subscribers/286010000000001")
                            .header("Authorization", "Bearer viewer-token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void operatorCannotPostSubscriber_returns403() throws Exception {
            stubAuthentication("operator-token", operatorUser);

            mockMvc.perform(post("/api/v1/subscribers")
                            .header("Authorization", "Bearer operator-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void viewerCanReadSubscriberCount_returns200() throws Exception {
            stubAuthentication("viewer-token", viewerUser);
            when(subscriberService.getSubscriberCount(TENANT)).thenReturn(10L);

            mockMvc.perform(get("/api/v1/subscribers/count")
                            .header("Authorization", "Bearer viewer-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("10"));
        }

        @Test
        void adminCanDeleteSubscriber_passes() throws Exception {
            stubAuthentication("admin-token", adminUser);

            mockMvc.perform(delete("/api/v1/subscribers/286010000000001")
                            .header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void operatorCanReadSubscriber_passes() throws Exception {
            stubAuthentication("operator-token", operatorUser);
            when(subscriberService.getSubscriberCount(TENANT)).thenReturn(5L);

            mockMvc.perform(get("/api/v1/subscribers/count")
                            .header("Authorization", "Bearer operator-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }
    }

    // ── Full Pipeline: Auth + RBAC combined ────────────────────────────

    @Nested
    class FullPipelineTests {

        @Test
        void validToken_correctRole_succeeds() throws Exception {
            stubAuthentication("admin-token", adminUser);
            when(subscriberService.getSubscriberCount(TENANT)).thenReturn(42L);

            mockMvc.perform(get("/api/v1/subscribers/count")
                            .header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("42"));
        }

        @Test
        void noToken_returnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/subscribers/count"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void validToken_wrongRole_rejected403() throws Exception {
            stubAuthentication("viewer-token", viewerUser);

            mockMvc.perform(post("/api/v1/subscribers")
                            .header("Authorization", "Bearer viewer-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

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
