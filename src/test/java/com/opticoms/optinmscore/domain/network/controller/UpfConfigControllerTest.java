package com.opticoms.optinmscore.domain.network.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.network.dto.UpfConfigRequest;
import com.opticoms.optinmscore.domain.network.dto.UpfConfigResponse;
import com.opticoms.optinmscore.domain.network.mapper.NetworkConfigMapper;
import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import com.opticoms.optinmscore.domain.network.service.UpfConfigService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UpfConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class UpfConfigControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE_URL = "/api/v1/network/config/upf";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UpfConfigService upfConfigService;
    @MockBean private NetworkConfigMapper networkConfigMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUpMapperStubs() {
        when(networkConfigMapper.toUpfConfigResponse(any(UpfConfig.class))).thenAnswer(inv -> {
            UpfConfig e = inv.getArgument(0);
            UpfConfigResponse r = new UpfConfigResponse();
            r.setId(e.getId());
            r.setN3InterfaceIp(e.getN3InterfaceIp());
            r.setS1uInterfaceIp(e.getS1uInterfaceIp());
            r.setN4PfcpIp(e.getN4PfcpIp());
            r.setCreatedAt(e.getCreatedAt());
            r.setUpdatedAt(e.getUpdatedAt());
            return r;
        });
        when(networkConfigMapper.toUpfConfigEntity(any(UpfConfigRequest.class))).thenAnswer(inv -> {
            UpfConfigRequest req = inv.getArgument(0);
            UpfConfig c = new UpfConfig();
            c.setN3InterfaceIp(req.getN3InterfaceIp());
            c.setS1uInterfaceIp(req.getS1uInterfaceIp());
            c.setN4PfcpIp(req.getN4PfcpIp());
            return c;
        });
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getUpf_returns200() throws Exception {
        UpfConfig config = new UpfConfig();
        config.setId("upf-doc-1");
        config.setTenantId(TENANT);
        config.setN3InterfaceIp("10.45.0.1");
        config.setS1uInterfaceIp("10.45.0.2");
        config.setN4PfcpIp("10.10.4.1");

        when(upfConfigService.getUpfConfig(TENANT)).thenReturn(config);

        mockMvc.perform(get(BASE_URL).requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("upf-doc-1"))
                .andExpect(jsonPath("$.n3InterfaceIp").value("10.45.0.1"))
                .andExpect(jsonPath("$.s1uInterfaceIp").value("10.45.0.2"))
                .andExpect(jsonPath("$.n4PfcpIp").value("10.10.4.1"));

        verify(upfConfigService).getUpfConfig(TENANT);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putUpf_returns200() throws Exception {
        UpfConfigRequest request = new UpfConfigRequest();
        request.setN3InterfaceIp("10.45.0.10");
        request.setS1uInterfaceIp("10.45.0.20");
        request.setN4PfcpIp("10.10.4.10");

        when(upfConfigService.saveOrUpdateUpfConfig(eq(TENANT), any(UpfConfig.class)))
                .thenAnswer(inv -> {
                    UpfConfig saved = inv.getArgument(1);
                    saved.setId("upf-doc-1");
                    saved.setTenantId(TENANT);
                    return saved;
                });

        mockMvc.perform(put(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("upf-doc-1"))
                .andExpect(jsonPath("$.n3InterfaceIp").value("10.45.0.10"))
                .andExpect(jsonPath("$.n4PfcpIp").value("10.10.4.10"));

        verify(upfConfigService).saveOrUpdateUpfConfig(eq(TENANT), any(UpfConfig.class));
    }
}
