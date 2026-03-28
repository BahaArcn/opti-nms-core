package com.opticoms.optinmscore.domain.license.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.license.model.License;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import com.opticoms.optinmscore.security.JwtService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LicenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class LicenseControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private LicenseService licenseService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createOrUpdate_returns201() throws Exception {
        License license = new License();
        license.setMaxSubscribers(100);
        license.setActive(true);

        when(licenseService.createOrUpdateLicense(eq(TENANT), any())).thenReturn(license);

        mockMvc.perform(post("/api/v1/licenses")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(license)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxSubscribers").value(100));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getLicense_returns200() throws Exception {
        License license = new License();
        license.setMaxSubscribers(100);

        when(licenseService.getLicense(TENANT)).thenReturn(license);

        mockMvc.perform(get("/api/v1/licenses")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxSubscribers").value(100));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteLicense_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/licenses")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(licenseService).deleteLicense(TENANT);
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void getLicenseStatus_returns200() throws Exception {
        LicenseService.LicenseStatus status = new LicenseService.LicenseStatus();
        status.setLicensePresent(true);
        status.setActive(true);

        when(licenseService.getLicenseStatus(TENANT)).thenReturn(status);

        mockMvc.perform(get("/api/v1/licenses/status")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePresent").value(true));
    }
}
