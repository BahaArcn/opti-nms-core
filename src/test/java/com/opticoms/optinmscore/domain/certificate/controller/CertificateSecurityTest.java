package com.opticoms.optinmscore.domain.certificate.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.certificate.dto.CertificateEntryResponse;
import com.opticoms.optinmscore.domain.certificate.mapper.CertificateEntryMapper;
import com.opticoms.optinmscore.domain.certificate.model.CertificateEntry;
import com.opticoms.optinmscore.domain.certificate.service.CertificateService;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CertificateController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class CertificateSecurityTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE = "/api/v1/certificates";
    private static final String CREATE_JSON = """
            {"name":"test-cert","certType":"SERVER","certificatePem":"-----BEGIN CERTIFICATE-----\\nMIIB\\n-----END CERTIFICATE-----"}
            """;

    @Autowired private MockMvc mockMvc;

    @MockBean private CertificateService certificateService;
    @MockBean private CertificateEntryMapper certificateEntryMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void noAuth_GET_403() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_GET_403() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);

        mockMvc.perform(get(BASE).header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorRole_GET_200() throws Exception {
        User operator = buildUser("operator", User.Role.OPERATOR);
        stubAuthentication("op-token", operator);
        when(certificateService.list(eq(TENANT), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(BASE).header("Authorization", "Bearer op-token"))
                .andExpect(status().isOk());
    }

    @Test
    void operatorRole_POST_403() throws Exception {
        User operator = buildUser("operator", User.Role.OPERATOR);
        stubAuthentication("op-token", operator);

        mockMvc.perform(post(BASE)
                        .header("Authorization", "Bearer op-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRole_POST_200() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN);
        stubAuthentication("admin-token", admin);

        CertificateEntry entity = new CertificateEntry();
        when(certificateEntryMapper.toEntity(any())).thenReturn(entity);
        when(certificateService.create(eq(TENANT), any(CertificateEntry.class))).thenReturn(entity);
        when(certificateEntryMapper.toResponse(any(CertificateEntry.class))).thenReturn(new CertificateEntryResponse());

        mockMvc.perform(post(BASE)
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isCreated());
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
