package com.opticoms.optinmscore.domain.license.controller;

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
