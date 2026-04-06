package com.opticoms.optinmscore.domain.network.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.network.dto.AmfConfigRequest;
import com.opticoms.optinmscore.domain.network.dto.AmfConfigResponse;
import com.opticoms.optinmscore.domain.network.mapper.NetworkConfigMapper;
import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import com.opticoms.optinmscore.domain.network.service.AmfConfigService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AmfConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class AmfConfigControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE_URL = "/api/v1/network/config/amf";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AmfConfigService amfConfigService;
    @MockBean private NetworkConfigMapper networkConfigMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUpMapperStubs() {
        when(networkConfigMapper.toAmfConfigResponse(any(AmfConfig.class))).thenAnswer(inv -> {
            AmfConfig e = inv.getArgument(0);
            AmfConfigResponse r = new AmfConfigResponse();
            r.setId(e.getId());
            r.setAmfName(e.getAmfName());
            r.setMmeName(e.getMmeName());
            r.setAmfId(e.getAmfId());
            r.setMmeId(e.getMmeId());
            r.setN2InterfaceIp(e.getN2InterfaceIp());
            r.setS1cInterfaceIp(e.getS1cInterfaceIp());
            r.setSupportedPlmns(e.getSupportedPlmns());
            r.setSupportedTais(e.getSupportedTais());
            r.setSupportedSlices(e.getSupportedSlices());
            r.setSecurityParameters(e.getSecurityParameters());
            r.setNasTimers5g(e.getNasTimers5g());
            r.setNasTimers4g(e.getNasTimers4g());
            r.setCreatedAt(e.getCreatedAt());
            r.setUpdatedAt(e.getUpdatedAt());
            return r;
        });
        when(networkConfigMapper.toAmfConfigEntity(any(AmfConfigRequest.class))).thenAnswer(inv -> {
            AmfConfigRequest req = inv.getArgument(0);
            AmfConfig c = new AmfConfig();
            c.setAmfName(req.getAmfName());
            c.setMmeName(req.getMmeName());
            c.setAmfId(req.getAmfId());
            c.setMmeId(req.getMmeId());
            c.setN2InterfaceIp(req.getN2InterfaceIp());
            c.setS1cInterfaceIp(req.getS1cInterfaceIp());
            c.setSupportedPlmns(req.getSupportedPlmns());
            c.setSupportedTais(req.getSupportedTais());
            c.setSupportedSlices(req.getSupportedSlices());
            if (req.getSecurityParameters() != null) {
                c.setSecurityParameters(req.getSecurityParameters());
            }
            if (req.getNasTimers5g() != null) {
                c.setNasTimers5g(req.getNasTimers5g());
            }
            if (req.getNasTimers4g() != null) {
                c.setNasTimers4g(req.getNasTimers4g());
            }
            return c;
        });
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getAmf_returns200() throws Exception {
        AmfConfig.Plmn plmn = new AmfConfig.Plmn();
        plmn.setMcc("286");
        plmn.setMnc("01");

        AmfConfig.Tai tai = new AmfConfig.Tai();
        tai.setPlmn(plmn);
        tai.setTac(1);
        tai.setTacEnd(1);

        AmfConfig config = new AmfConfig();
        config.setId("amf-doc-1");
        config.setTenantId(TENANT);
        config.setAmfName("test-amf");
        config.setN2InterfaceIp("10.0.0.1");
        config.setSupportedPlmns(List.of(plmn));
        config.setSupportedTais(List.of(tai));

        when(amfConfigService.getAmfConfig(TENANT)).thenReturn(config);

        mockMvc.perform(get(BASE_URL).requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("amf-doc-1"))
                .andExpect(jsonPath("$.amfName").value("test-amf"))
                .andExpect(jsonPath("$.n2InterfaceIp").value("10.0.0.1"))
                .andExpect(jsonPath("$.supportedPlmns[0].mcc").value("286"));

        verify(amfConfigService).getAmfConfig(TENANT);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putAmf_returns200() throws Exception {
        AmfConfig.Plmn plmn = new AmfConfig.Plmn();
        plmn.setMcc("286");
        plmn.setMnc("01");

        AmfConfig.Tai tai = new AmfConfig.Tai();
        tai.setPlmn(plmn);
        tai.setTac(100);
        tai.setTacEnd(100);

        AmfConfigRequest request = new AmfConfigRequest();
        request.setAmfName("updated-amf");
        request.setN2InterfaceIp("10.0.0.2");
        request.setSupportedPlmns(List.of(plmn));
        request.setSupportedTais(List.of(tai));

        when(amfConfigService.saveOrUpdateAmfConfig(eq(TENANT), any(AmfConfig.class)))
                .thenAnswer(inv -> {
                    AmfConfig saved = inv.getArgument(1);
                    saved.setId("amf-doc-1");
                    saved.setTenantId(TENANT);
                    return saved;
                });

        mockMvc.perform(put(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("amf-doc-1"))
                .andExpect(jsonPath("$.amfName").value("updated-amf"))
                .andExpect(jsonPath("$.n2InterfaceIp").value("10.0.0.2"));

        verify(amfConfigService).saveOrUpdateAmfConfig(eq(TENANT), any(AmfConfig.class));
    }
}
