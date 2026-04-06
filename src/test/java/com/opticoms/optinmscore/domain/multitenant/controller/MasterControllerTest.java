package com.opticoms.optinmscore.domain.multitenant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.multitenant.dto.SlaveNodeResponse;
import com.opticoms.optinmscore.domain.multitenant.mapper.SlaveNodeMapper;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode.SlaveStatus;
import com.opticoms.optinmscore.domain.multitenant.service.MasterService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MasterController.class)
@AutoConfigureMockMvc(addFilters = false)
class MasterControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private MasterService masterService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private SlaveNodeMapper slaveNodeMapper;

    private static final String TENANT = "OPTC-0001/0001/01";

    @BeforeEach
    void setUp() {
        when(slaveNodeMapper.toResponse(any(SlaveNode.class))).thenAnswer(inv -> {
            SlaveNode e = inv.getArgument(0);
            SlaveNodeResponse r = new SlaveNodeResponse();
            r.setId(e.getId());
            r.setSlaveAddress(e.getSlaveAddress());
            r.setSlaveTenantId(e.getSlaveTenantId());
            r.setStatus(e.getStatus());
            r.setLastHeartbeat(e.getLastHeartbeat());
            r.setRegisteredAt(e.getRegisteredAt());
            return r;
        });
    }

    private SlaveNode buildNode() {
        SlaveNode node = new SlaveNode();
        node.setId("node-1");
        node.setTenantId(TENANT);
        node.setSlaveAddress("http://slave1:8080");
        node.setSlaveTenantId("slave-t1");
        node.setStatus(SlaveStatus.ONLINE);
        node.setRegisteredAt(new Date());
        node.setLastHeartbeat(new Date());
        return node;
    }

    @Test
    @DisplayName("POST /register - returns 201")
    @WithMockUser(roles = "ADMIN")
    void registerSlave_returns201() throws Exception {
        when(masterService.registerSlave(eq(TENANT), anyString(), anyString()))
                .thenReturn(buildNode());

        String body = objectMapper.writeValueAsString(
                new MasterController.RegisterSlaveRequest() {{
                    setSlaveAddress("http://slave1:8080");
                    setSlaveTenantId("slave-t1");
                }});

        mockMvc.perform(post("/api/v1/master/slaves/register")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slaveAddress").value("http://slave1:8080"));
    }

    @Test
    @DisplayName("DELETE /{slaveAddress} - returns 204")
    @WithMockUser(roles = "ADMIN")
    void deregisterSlave_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/master/slaves/slave1-node")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(masterService).deregisterSlave(TENANT, "slave1-node");
    }

    @Test
    @DisplayName("POST /heartbeat - returns 204")
    @WithMockUser(roles = "ADMIN")
    void heartbeat_returns204() throws Exception {
        String body = objectMapper.writeValueAsString(
                new MasterController.HeartbeatRequest() {{
                    setSlaveAddress("http://slave1:8080");
                }});

        mockMvc.perform(post("/api/v1/master/slaves/heartbeat")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(masterService).heartbeat(eq(TENANT), eq("http://slave1:8080"));
    }

    @Test
    @DisplayName("GET / - returns 200 with page")
    @WithMockUser(roles = "ADMIN")
    void listSlaves_returns200WithPage() throws Exception {
        when(masterService.listSlaves(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildNode())));

        mockMvc.perform(get("/api/v1/master/slaves")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slaveAddress").value("http://slave1:8080"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /push-config - returns 200 with count")
    @WithMockUser(roles = "ADMIN")
    void pushConfig_returns200WithCount() throws Exception {
        SlaveNode onlineNode = buildNode();
        when(masterService.listSlaves(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(onlineNode)));

        mockMvc.perform(post("/api/v1/master/slaves/push-config")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pushed").value(1));

        verify(masterService).pushConfigToAllSlaves(TENANT);
    }
}
