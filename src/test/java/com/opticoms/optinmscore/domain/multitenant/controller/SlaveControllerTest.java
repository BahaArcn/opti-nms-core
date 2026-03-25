package com.opticoms.optinmscore.domain.multitenant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.multitenant.model.MultiTenantConfigPayload;
import com.opticoms.optinmscore.domain.multitenant.service.SlaveClientService;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SlaveController.class)
@AutoConfigureMockMvc(addFilters = false)
class SlaveControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SlaveClientService slaveClientService;
    @MockBean private GlobalConfigRepository globalConfigRepository;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private static final String TENANT = "slave-tenant";

    @Test
    @DisplayName("POST /config - returns 204")
    @WithMockUser
    void applyConfig_returns204() throws Exception {
        MultiTenantConfigPayload payload = new MultiTenantConfigPayload();
        payload.setNetworkFullName("MasterNet");
        payload.setNetworkShortName("MN");
        payload.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);

        mockMvc.perform(post("/api/v1/slave/config")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNoContent());

        verify(slaveClientService).applyConfigFromMaster(TENANT, payload);
    }

    @Test
    @DisplayName("GET /status - returns 200")
    @WithMockUser
    void getStatus_returns200() throws Exception {
        GlobalConfig config = new GlobalConfig();
        config.setTenantId(TENANT);
        config.setWorkAsMaster(false);
        config.setMasterAddr("http://master:8080");
        when(globalConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.of(config));
        when(slaveClientService.getSelfAddress()).thenReturn("http://localhost:8080");

        mockMvc.perform(get("/api/v1/slave/status")
                        .header("X-Tenant-ID", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workAsMaster").value(false))
                .andExpect(jsonPath("$.masterAddr").value("http://master:8080"))
                .andExpect(jsonPath("$.selfAddress").value("http://localhost:8080"))
                .andExpect(jsonPath("$.tenantId").value(TENANT));
    }
}
