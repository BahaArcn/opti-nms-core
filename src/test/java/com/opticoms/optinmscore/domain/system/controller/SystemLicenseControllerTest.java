package com.opticoms.optinmscore.domain.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.license.dto.LicenseRequest;
import com.opticoms.optinmscore.domain.license.dto.LicenseResponse;
import com.opticoms.optinmscore.domain.license.mapper.LicenseMapper;
import com.opticoms.optinmscore.domain.license.model.License;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import com.opticoms.optinmscore.security.JwtService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
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

@WebMvcTest(SystemLicenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemLicenseControllerTest {

    private static final String TENANT = "TURK-0001/0001/01";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private LicenseService licenseService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private LicenseMapper licenseMapper;

    @BeforeEach
    void setUp() {
        when(licenseMapper.toResponse(any(License.class))).thenAnswer(inv -> {
            License e = inv.getArgument(0);
            LicenseResponse r = new LicenseResponse();
            r.setMaxSubscribers(e.getMaxSubscribers());
            r.setActive(e.isActive());
            return r;
        });
        when(licenseMapper.toEntity(any(LicenseRequest.class))).thenAnswer(inv -> {
            LicenseRequest req = inv.getArgument(0);
            License l = new License();
            l.setMaxSubscribers(req.getMaxSubscribers());
            l.setActive(req.isActive());
            return l;
        });
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createOrUpdate_returns201() throws Exception {
        License license = new License();
        license.setMaxSubscribers(1000);
        license.setActive(true);

        when(licenseService.createOrUpdateLicense(eq(TENANT), any())).thenReturn(license);

        mockMvc.perform(post("/api/v1/system/licenses")
                        .param("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(license)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxSubscribers").value(1000));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void deleteLicense_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/system/licenses")
                        .param("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(licenseService).deleteLicense(TENANT);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getLicense_returns200() throws Exception {
        License license = new License();
        license.setMaxSubscribers(500);

        when(licenseService.getLicense(TENANT)).thenReturn(license);

        mockMvc.perform(get("/api/v1/system/licenses")
                        .param("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxSubscribers").value(500));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getLicenseStatus_returns200() throws Exception {
        LicenseService.LicenseStatus status = new LicenseService.LicenseStatus();
        status.setLicensePresent(true);
        status.setActive(true);
        status.setCurrentSubscribers(42);

        when(licenseService.getLicenseStatus(TENANT)).thenReturn(status);

        mockMvc.perform(get("/api/v1/system/licenses/status")
                        .param("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePresent").value(true))
                .andExpect(jsonPath("$.currentSubscribers").value(42));
    }
}
