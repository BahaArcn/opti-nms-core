package com.opticoms.optinmscore.domain.firewall.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.firewall.dto.FirewallRuleResponse;
import com.opticoms.optinmscore.domain.firewall.mapper.FirewallRuleMapper;
import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import com.opticoms.optinmscore.domain.firewall.service.FirewallService;
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

@WebMvcTest(FirewallController.class)
@AutoConfigureMockMvc(addFilters = false)
class FirewallControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE_URL = "/api/v1/firewall/rules";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private FirewallService firewallService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private FirewallRuleMapper firewallRuleMapper;

    @BeforeEach
    void setUpMapperStubs() {
        when(firewallRuleMapper.toEntity(any())).thenReturn(new FirewallRule());
        when(firewallRuleMapper.toResponse(any())).thenAnswer(inv -> {
            FirewallRule e = inv.getArgument(0);
            if (e == null) return null;
            FirewallRuleResponse r = new FirewallRuleResponse();
            r.setId(e.getId());
            r.setChain(e.getChain());
            r.setProtocol(e.getProtocol());
            r.setDestinationPort(e.getDestinationPort());
            r.setAction(e.getAction());
            r.setDescription(e.getDescription());
            r.setPriority(e.getPriority());
            r.setEnabled(e.isEnabled());
            r.setRuleStatus(e.getRuleStatus());
            return r;
        });
        when(firewallRuleMapper.toResponseList(any())).thenAnswer(inv -> {
            List<FirewallRule> entities = inv.getArgument(0);
            return entities.stream().map(e -> {
                FirewallRuleResponse r = new FirewallRuleResponse();
                r.setId(e.getId());
                r.setChain(e.getChain());
                r.setAction(e.getAction());
                r.setDescription(e.getDescription());
                r.setEnabled(e.isEnabled());
                r.setRuleStatus(e.getRuleStatus());
                return r;
            }).toList();
        });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRule_returns201() throws Exception {
        FirewallRule rule = buildRule();
        when(firewallService.createRule(eq(TENANT), any())).thenReturn(rule);

        mockMvc.perform(post(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rule)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chain").value("INPUT"))
                .andExpect(jsonPath("$.action").value("ACCEPT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRules_returns200() throws Exception {
        when(firewallService.listRules(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildRule())));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].chain").value("INPUT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRules_enabledFilter_returns200() throws Exception {
        when(firewallService.listEnabledRules(TENANT)).thenReturn(List.of(buildRule()));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRule_returns200() throws Exception {
        when(firewallService.getRuleById(TENANT, "rule-1")).thenReturn(buildRule());

        mockMvc.perform(get(BASE_URL + "/rule-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Allow HTTP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listByChain_returns200() throws Exception {
        when(firewallService.listRulesByChain(TENANT, FirewallRule.Chain.INPUT))
                .thenReturn(List.of(buildRule()));

        mockMvc.perform(get(BASE_URL + "/chain/INPUT")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chain").value("INPUT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listByStatus_returns200() throws Exception {
        when(firewallService.listRulesByStatus(TENANT, FirewallRule.RuleStatus.APPLIED))
                .thenReturn(List.of(buildRule()));

        mockMvc.perform(get(BASE_URL + "/status/APPLIED")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRule_returns200() throws Exception {
        FirewallRule rule = buildRule();
        when(firewallService.updateRule(eq(TENANT), eq("rule-1"), any())).thenReturn(rule);

        mockMvc.perform(put(BASE_URL + "/rule-1")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rule)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRule_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/rule-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(firewallService).deleteRule(TENANT, "rule-1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void applyRule_returns200() throws Exception {
        when(firewallService.applyRuleToOs(TENANT, "rule-1")).thenReturn(buildRule());

        mockMvc.perform(post(BASE_URL + "/rule-1/apply")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeRule_returns200() throws Exception {
        when(firewallService.removeRuleFromOs(TENANT, "rule-1")).thenReturn(buildRule());

        mockMvc.perform(post(BASE_URL + "/rule-1/remove")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void countRules_returns200() throws Exception {
        when(firewallService.countRules(TENANT)).thenReturn(42L);

        mockMvc.perform(get(BASE_URL + "/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private FirewallRule buildRule() {
        FirewallRule r = new FirewallRule();
        r.setId("rule-1");
        r.setTenantId(TENANT);
        r.setChain(FirewallRule.Chain.INPUT);
        r.setProtocol(FirewallRule.Protocol.TCP);
        r.setDestinationPort(80);
        r.setAction(FirewallRule.Action.ACCEPT);
        r.setDescription("Allow HTTP");
        r.setPriority(100);
        r.setEnabled(true);
        r.setRuleStatus(FirewallRule.RuleStatus.PENDING);
        return r;
    }
}
