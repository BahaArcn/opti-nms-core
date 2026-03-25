package com.opticoms.optinmscore.domain.inventory.controller;

import com.opticoms.optinmscore.domain.inventory.model.GNodeB;
import com.opticoms.optinmscore.domain.inventory.service.InventoryService;
import com.opticoms.optinmscore.security.JwtService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;

    @MockBean private InventoryService inventoryService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "VIEWER")
    void listGNodeBs_returns200() throws Exception {
        GNodeB gnb = new GNodeB();
        gnb.setGnbId("gnb-001");
        gnb.setStatus(GNodeB.ConnectionStatus.CONNECTED);

        when(inventoryService.getAllGNodeBsPaged(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(gnb)));

        mockMvc.perform(get("/api/v1/inventory/gnb")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].gnbId").value("gnb-001"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getGNodeB_returns200() throws Exception {
        GNodeB gnb = new GNodeB();
        gnb.setGnbId("gnb-001");
        when(inventoryService.getGNodeB(TENANT, "gnb-001")).thenReturn(gnb);

        mockMvc.perform(get("/api/v1/inventory/gnb/gnb-001")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gnbId").value("gnb-001"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getGNodeBCount_returns200() throws Exception {
        when(inventoryService.getGNodeBCount(TENANT)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/inventory/gnb/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getConnectedUeCount_returns200() throws Exception {
        when(inventoryService.getConnectedUeCount(TENANT)).thenReturn(10L);

        mockMvc.perform(get("/api/v1/inventory/ue/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getActiveSessionCount_returns200() throws Exception {
        when(inventoryService.getActiveSessionCount(TENANT)).thenReturn(8L);

        mockMvc.perform(get("/api/v1/inventory/session/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("8"));
    }
}
