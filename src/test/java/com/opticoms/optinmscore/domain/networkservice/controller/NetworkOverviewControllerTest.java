package com.opticoms.optinmscore.domain.networkservice.controller;

import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse.ServiceStatusItem;
import com.opticoms.optinmscore.domain.networkservice.service.NetworkOverviewService;
import com.opticoms.optinmscore.security.JwtService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NetworkOverviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class NetworkOverviewControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;

    @MockBean private NetworkOverviewService networkOverviewService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOverview_admin_returnsOk() throws Exception {
        NetworkOverviewResponse response = NetworkOverviewResponse.builder()
                .services(List.of(
                        ServiceStatusItem.builder()
                                .name("Control Plane").type("CONTROL_PLANE")
                                .status("RUNNING").statusMessage("The service is running.")
                                .build(),
                        ServiceStatusItem.builder()
                                .name("Policy & Subscriber Mgmt.").type("POLICY_SUBSCRIBER_MGMT")
                                .status("RUNNING").statusMessage("The service is running.")
                                .build(),
                        ServiceStatusItem.builder()
                                .name("Data Plane").type("DATA_PLANE")
                                .status("UNKNOWN").statusMessage("No URL configured.")
                                .build()
                ))
                .build();

        when(networkOverviewService.getOverview(TENANT)).thenReturn(response);

        mockMvc.perform(get("/api/v1/networks/overview")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services.length()").value(3))
                .andExpect(jsonPath("$.services[0].name").value("Control Plane"))
                .andExpect(jsonPath("$.services[0].type").value("CONTROL_PLANE"))
                .andExpect(jsonPath("$.services[0].status").value("RUNNING"))
                .andExpect(jsonPath("$.services[1].name").value("Policy & Subscriber Mgmt."))
                .andExpect(jsonPath("$.services[2].status").value("UNKNOWN"))
                .andExpect(jsonPath("$.services[2].statusMessage").value("No URL configured."));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getOverview_viewer_returnsOk() throws Exception {
        NetworkOverviewResponse response = NetworkOverviewResponse.builder()
                .services(List.of())
                .build();

        when(networkOverviewService.getOverview(TENANT)).thenReturn(response);

        mockMvc.perform(get("/api/v1/networks/overview")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk());
    }

}
