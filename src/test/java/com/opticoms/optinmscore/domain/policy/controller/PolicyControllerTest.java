package com.opticoms.optinmscore.domain.policy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.policy.dto.PolicyRequest;
import com.opticoms.optinmscore.domain.policy.dto.PolicyResponse;
import com.opticoms.optinmscore.domain.policy.mapper.PolicyMapper;
import com.opticoms.optinmscore.domain.policy.model.Policy;
import com.opticoms.optinmscore.domain.policy.service.PolicyService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE_URL = "/api/v1/policies";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PolicyService policyService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private PolicyMapper policyMapper;

    @BeforeEach
    void setUpMapperStubs() {
        when(policyMapper.toResponse(any(Policy.class))).thenAnswer(inv -> {
            Policy e = inv.getArgument(0);
            PolicyResponse r = new PolicyResponse();
            r.setId(e.getId());
            r.setName(e.getName());
            r.setDescription(e.getDescription());
            r.setEnabled(e.isEnabled());
            r.setBandwidthLimit(e.getBandwidthLimit());
            r.setRatPreference(e.getRatPreference());
            r.setFrequencySelectionPriority(e.getFrequencySelectionPriority());
            r.setIpFilteringEnabled(e.isIpFilteringEnabled());
            r.setIpFilterRules(e.getIpFilterRules());
            r.setAllowedTacs(e.getAllowedTacs());
            r.setTimeSchedule(e.getTimeSchedule());
            r.setDefaultSlices(e.getDefaultSlices());
            r.setCreatedAt(e.getCreatedAt());
            r.setUpdatedAt(e.getUpdatedAt());
            return r;
        });
        when(policyMapper.toEntity(any(PolicyRequest.class))).thenAnswer(inv -> {
            PolicyRequest req = inv.getArgument(0);
            Policy p = new Policy();
            p.setName(req.getName());
            p.setDescription(req.getDescription());
            p.setEnabled(req.isEnabled());
            p.setBandwidthLimit(req.getBandwidthLimit());
            p.setRatPreference(req.getRatPreference());
            p.setFrequencySelectionPriority(req.getFrequencySelectionPriority());
            p.setIpFilteringEnabled(req.isIpFilteringEnabled());
            p.setIpFilterRules(req.getIpFilterRules());
            p.setAllowedTacs(req.getAllowedTacs());
            p.setTimeSchedule(req.getTimeSchedule());
            p.setDefaultSlices(req.getDefaultSlices());
            return p;
        });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201() throws Exception {
        PolicyRequest request = new PolicyRequest();
        request.setName("Gold Plan");
        request.setDescription("Enterprise policy");

        when(policyService.createPolicy(eq(TENANT), any(Policy.class))).thenAnswer(inv -> {
            Policy p = inv.getArgument(1);
            p.setId("pol-1");
            p.setTenantId(TENANT);
            return p;
        });

        mockMvc.perform(post(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pol-1"))
                .andExpect(jsonPath("$.name").value("Gold Plan"))
                .andExpect(jsonPath("$.description").value("Enterprise policy"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void list_returns200() throws Exception {
        Policy policy = new Policy();
        policy.setId("pol-1");
        policy.setName("Gold Plan");

        when(policyService.listPolicies(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(policy)));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Gold Plan"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void get_returns200() throws Exception {
        Policy policy = new Policy();
        policy.setId("pol-1");
        policy.setName("Gold Plan");
        policy.setDescription("Enterprise policy");

        when(policyService.getPolicy(TENANT, "pol-1")).thenReturn(policy);

        mockMvc.perform(get(BASE_URL + "/pol-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pol-1"))
                .andExpect(jsonPath("$.name").value("Gold Plan"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns200() throws Exception {
        PolicyRequest request = new PolicyRequest();
        request.setName("Gold Plan Plus");
        request.setDescription("Updated");

        Policy updated = new Policy();
        updated.setId("pol-1");
        updated.setName("Gold Plan Plus");
        updated.setDescription("Updated");

        when(policyService.updatePolicy(eq(TENANT), eq("pol-1"), any(Policy.class)))
                .thenReturn(updated);

        mockMvc.perform(put(BASE_URL + "/pol-1")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gold Plan Plus"))
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/pol-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(policyService).deletePolicy(TENANT, "pol-1");
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void count_returns200() throws Exception {
        when(policyService.countPolicies(TENANT)).thenReturn(7L);

        mockMvc.perform(get(BASE_URL + "/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }
}
