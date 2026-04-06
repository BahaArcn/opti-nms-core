package com.opticoms.optinmscore.domain.network.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.network.dto.SmfConfigRequest;
import com.opticoms.optinmscore.domain.network.dto.SmfConfigResponse;
import com.opticoms.optinmscore.domain.network.mapper.NetworkConfigMapper;
import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import com.opticoms.optinmscore.domain.network.service.SmfConfigService;
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

@WebMvcTest(SmfConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class SmfConfigControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE_URL = "/api/v1/network/config/smf";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SmfConfigService smfConfigService;
    @MockBean private NetworkConfigMapper networkConfigMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUpMapperStubs() {
        when(networkConfigMapper.toSmfConfigResponse(any(SmfConfig.class))).thenAnswer(inv -> {
            SmfConfig e = inv.getArgument(0);
            SmfConfigResponse r = new SmfConfigResponse();
            r.setId(e.getId());
            r.setSmfMtu(e.getSmfMtu());
            r.setSmfDnsIps(e.getSmfDnsIps());
            r.setSecurityIndication(e.getSecurityIndication());
            r.setTcpMss(e.getTcpMss());
            r.setDhcpLeaseTimeSec(e.getDhcpLeaseTimeSec());
            r.setProxyCscfIp(e.getProxyCscfIp());
            r.setApnList(e.getApnList());
            r.setCreatedAt(e.getCreatedAt());
            r.setUpdatedAt(e.getUpdatedAt());
            return r;
        });
        when(networkConfigMapper.toSmfConfigEntity(any(SmfConfigRequest.class))).thenAnswer(inv -> {
            SmfConfigRequest req = inv.getArgument(0);
            SmfConfig c = new SmfConfig();
            c.setSmfMtu(req.getSmfMtu());
            c.setSmfDnsIps(req.getSmfDnsIps());
            if (req.getSecurityIndication() != null) {
                c.setSecurityIndication(req.getSecurityIndication());
            }
            c.setTcpMss(req.getTcpMss());
            c.setDhcpLeaseTimeSec(req.getDhcpLeaseTimeSec());
            c.setProxyCscfIp(req.getProxyCscfIp());
            c.setApnList(req.getApnList());
            return c;
        });
    }

    private static SmfConfig.ApnDnn sampleApn() {
        SmfConfig.ApnDnn apn = new SmfConfig.ApnDnn();
        apn.setTunInterface("ogstun");
        apn.setApnDnnName("internet");
        return apn;
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getSmf_returns200() throws Exception {
        SmfConfig config = new SmfConfig();
        config.setId("smf-doc-1");
        config.setTenantId(TENANT);
        config.setSmfMtu(1400);
        config.setTcpMss(1340);
        config.setDhcpLeaseTimeSec(7200);
        config.setApnList(List.of(sampleApn()));

        when(smfConfigService.getSmfConfig(TENANT)).thenReturn(config);

        mockMvc.perform(get(BASE_URL).requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("smf-doc-1"))
                .andExpect(jsonPath("$.smfMtu").value(1400))
                .andExpect(jsonPath("$.tcpMss").value(1340))
                .andExpect(jsonPath("$.apnList[0].apnDnnName").value("internet"));

        verify(smfConfigService).getSmfConfig(TENANT);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putSmf_returns200() throws Exception {
        SmfConfigRequest request = new SmfConfigRequest();
        request.setSmfMtu(1450);
        request.setTcpMss(1340);
        request.setDhcpLeaseTimeSec(7200);
        request.setApnList(List.of(sampleApn()));

        when(smfConfigService.saveOrUpdateSmfConfig(eq(TENANT), any(SmfConfig.class)))
                .thenAnswer(inv -> {
                    SmfConfig saved = inv.getArgument(1);
                    saved.setId("smf-doc-1");
                    saved.setTenantId(TENANT);
                    return saved;
                });

        mockMvc.perform(put(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("smf-doc-1"))
                .andExpect(jsonPath("$.smfMtu").value(1450))
                .andExpect(jsonPath("$.apnList[0].tunInterface").value("ogstun"));

        verify(smfConfigService).saveOrUpdateSmfConfig(eq(TENANT), any(SmfConfig.class));
    }
}
