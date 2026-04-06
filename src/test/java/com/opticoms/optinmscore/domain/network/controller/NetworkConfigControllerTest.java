package com.opticoms.optinmscore.domain.network.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.network.dto.GlobalConfigRequest;
import com.opticoms.optinmscore.domain.network.dto.GlobalConfigResponse;
import com.opticoms.optinmscore.domain.network.mapper.NetworkConfigMapper;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.service.NetworkConfigService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NetworkConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class NetworkConfigControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE_URL = "/api/v1/network/config";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NetworkConfigService networkConfigService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private NetworkConfigMapper networkConfigMapper;

    @BeforeEach
    void setUpMapperStubs() {
        when(networkConfigMapper.toGlobalConfigResponse(any(GlobalConfig.class))).thenAnswer(inv -> {
            GlobalConfig e = inv.getArgument(0);
            GlobalConfigResponse r = new GlobalConfigResponse();
            r.setId(e.getId());
            r.setNetworkFullName(e.getNetworkFullName());
            r.setNetworkShortName(e.getNetworkShortName());
            r.setNetworkMode(e.getNetworkMode());
            r.setMaxSupportedDevices(e.getMaxSupportedDevices());
            r.setMaxSupportedGNBs(e.getMaxSupportedGNBs());
            r.setWorkAsMaster(e.isWorkAsMaster());
            r.setMasterAddr(e.getMasterAddr());
            r.setTaiList(e.getTaiList());
            r.setMtu(e.getMtu());
            r.setDnsIps(e.getDnsIps());
            r.setUeIpPoolList(e.getUeIpPoolList());
            r.setDefaultFiveQi(e.getDefaultFiveQi());
            r.setDefaultArpPriority(e.getDefaultArpPriority());
            r.setDefaultAmbrUlKbps(e.getDefaultAmbrUlKbps());
            r.setDefaultAmbrDlKbps(e.getDefaultAmbrDlKbps());
            r.setUdmAmf(e.getUdmAmf());
            r.setAuthMethod(e.getAuthMethod());
            r.setEncryptClientSignaling(e.isEncryptClientSignaling());
            r.setCreatedAt(e.getCreatedAt());
            r.setUpdatedAt(e.getUpdatedAt());
            return r;
        });
        when(networkConfigMapper.toGlobalConfigEntity(any(GlobalConfigRequest.class))).thenAnswer(inv -> {
            GlobalConfigRequest req = inv.getArgument(0);
            GlobalConfig c = new GlobalConfig();
            c.setNetworkFullName(req.getNetworkFullName());
            c.setNetworkShortName(req.getNetworkShortName());
            c.setNetworkMode(req.getNetworkMode());
            c.setWorkAsMaster(req.isWorkAsMaster());
            c.setMasterAddr(req.getMasterAddr());
            c.setTaiList(req.getTaiList());
            c.setMtu(req.getMtu());
            c.setDnsIps(req.getDnsIps());
            c.setUeIpPoolList(req.getUeIpPoolList());
            c.setDefaultFiveQi(req.getDefaultFiveQi());
            c.setDefaultArpPriority(req.getDefaultArpPriority());
            c.setDefaultAmbrUlKbps(req.getDefaultAmbrUlKbps());
            c.setDefaultAmbrDlKbps(req.getDefaultAmbrDlKbps());
            c.setUdmAmf(req.getUdmAmf());
            c.setAuthMethod(req.getAuthMethod());
            c.setEncryptClientSignaling(req.isEncryptClientSignaling());
            return c;
        });
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getGlobal_returns200() throws Exception {
        GlobalConfig config = new GlobalConfig();
        config.setId("gc-1");
        config.setTenantId(TENANT);
        config.setNetworkFullName("OptiNMS Lab Network");
        config.setNetworkShortName("OPT-LAB");
        config.setNetworkMode(GlobalConfig.NetworkMode.HYBRID_4G_5G);

        when(networkConfigService.getGlobalConfig(TENANT)).thenReturn(config);

        mockMvc.perform(get(BASE_URL + "/global")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("gc-1"))
                .andExpect(jsonPath("$.networkFullName").value("OptiNMS Lab Network"))
                .andExpect(jsonPath("$.networkShortName").value("OPT-LAB"))
                .andExpect(jsonPath("$.networkMode").value("HYBRID_4G_5G"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void putGlobal_returns200() throws Exception {
        GlobalConfigRequest request = new GlobalConfigRequest();
        request.setNetworkFullName("OptiNMS Lab Network");
        request.setNetworkShortName("OPT-LAB");
        request.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);
        request.setAuthMethod(GlobalConfig.AuthMethod.FIVE_G_AKA);

        when(networkConfigService.saveOrUpdateGlobalConfig(eq(TENANT), any(GlobalConfig.class)))
                .thenAnswer(inv -> {
                    GlobalConfig saved = inv.getArgument(1);
                    saved.setId("gc-1");
                    saved.setTenantId(TENANT);
                    return saved;
                });

        mockMvc.perform(put(BASE_URL + "/global")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("gc-1"))
                .andExpect(jsonPath("$.networkFullName").value("OptiNMS Lab Network"))
                .andExpect(jsonPath("$.networkMode").value("ONLY_5G"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getIpPools_returns200() throws Exception {
        GlobalConfig.UeIpPool pool = new GlobalConfig.UeIpPool();
        pool.setIpRange("10.45.0.0/16");
        pool.setTunInterface("ogstun");
        pool.setGatewayIp("10.45.0.1");

        GlobalConfig config = new GlobalConfig();
        config.setUeIpPoolList(List.of(pool));

        when(networkConfigService.getGlobalConfig(TENANT)).thenReturn(config);

        mockMvc.perform(get(BASE_URL + "/global/ip-pools")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tunInterface").value("ogstun"))
                .andExpect(jsonPath("$[0].gatewayIp").value("10.45.0.1"));
    }
}
