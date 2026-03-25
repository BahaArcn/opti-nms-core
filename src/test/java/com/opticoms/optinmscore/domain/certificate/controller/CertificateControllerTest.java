package com.opticoms.optinmscore.domain.certificate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.certificate.model.CertificateEntry;
import com.opticoms.optinmscore.domain.certificate.service.CertificateService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CertificateController.class)
@AutoConfigureMockMvc(addFilters = false)
class CertificateControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE_URL = "/api/v1/certificates";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CertificateService certificateService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201() throws Exception {
        CertificateEntry entry = buildEntry();
        when(certificateService.create(eq(TENANT), any())).thenReturn(entry);

        mockMvc.perform(post(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("amf-server-cert"))
                .andExpect(jsonPath("$.certType").value("SERVER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_returns200() throws Exception {
        when(certificateService.list(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildEntry())));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("amf-server-cert"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_returns200() throws Exception {
        when(certificateService.getById(TENANT, "cert-1")).thenReturn(buildEntry());

        mockMvc.perform(get(BASE_URL + "/cert-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjectDn").value("CN=test.open5gs.org"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listByType_returns200() throws Exception {
        when(certificateService.listByType(TENANT, CertificateEntry.CertType.SERVER))
                .thenReturn(List.of(buildEntry()));

        mockMvc.perform(get(BASE_URL + "/type/SERVER")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].certType").value("SERVER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listByStatus_returns200() throws Exception {
        when(certificateService.listByStatus(TENANT, CertificateEntry.CertStatus.ACTIVE))
                .thenReturn(List.of(buildEntry()));

        mockMvc.perform(get(BASE_URL + "/status/ACTIVE")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listExpiring_returns200() throws Exception {
        when(certificateService.listExpiringSoon(TENANT, 60))
                .thenReturn(List.of(buildEntry()));

        mockMvc.perform(get(BASE_URL + "/expiring")
                        .requestAttr("tenantId", TENANT)
                        .param("withinDays", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("amf-server-cert"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns200() throws Exception {
        CertificateEntry entry = buildEntry();
        when(certificateService.update(eq(TENANT), eq("cert-1"), any())).thenReturn(entry);

        mockMvc.perform(put(BASE_URL + "/cert-1")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/cert-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(certificateService).delete(TENANT, "cert-1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revoke_returns200() throws Exception {
        CertificateEntry revoked = buildEntry();
        revoked.setStatus(CertificateEntry.CertStatus.REVOKED);
        when(certificateService.revoke(TENANT, "cert-1")).thenReturn(revoked);

        mockMvc.perform(post(BASE_URL + "/cert-1/revoke")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void count_returns200() throws Exception {
        when(certificateService.count(TENANT)).thenReturn(12L);

        mockMvc.perform(get(BASE_URL + "/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("12"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void list_operatorRole_returns200() throws Exception {
        when(certificateService.list(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildEntry())));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk());
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private CertificateEntry buildEntry() {
        CertificateEntry e = new CertificateEntry();
        e.setId("cert-1");
        e.setTenantId(TENANT);
        e.setName("amf-server-cert");
        e.setCertType(CertificateEntry.CertType.SERVER);
        e.setCertificatePem("-----BEGIN CERTIFICATE-----\nMIIB...\n-----END CERTIFICATE-----");
        e.setSubjectDn("CN=test.open5gs.org");
        e.setIssuerDn("CN=OptiNMS CA");
        e.setSerialNumber("1a2b3c");
        e.setNotBefore(System.currentTimeMillis() - 86400000L);
        e.setNotAfter(System.currentTimeMillis() + 365L * 86400000L);
        e.setStatus(CertificateEntry.CertStatus.ACTIVE);
        e.setDescription("AMF server TLS cert");
        return e;
    }
}
