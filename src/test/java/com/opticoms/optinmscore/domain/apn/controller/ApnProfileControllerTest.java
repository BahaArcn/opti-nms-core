package com.opticoms.optinmscore.domain.apn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.apn.dto.ApnProfileResponse;
import com.opticoms.optinmscore.domain.apn.mapper.ApnProfileMapper;
import com.opticoms.optinmscore.domain.apn.model.ApnProfile;
import com.opticoms.optinmscore.domain.apn.service.ApnProfileService;
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

@WebMvcTest(ApnProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApnProfileControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE_URL = "/api/v1/apn/profiles";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ApnProfileService apnProfileService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private ApnProfileMapper apnProfileMapper;

    @BeforeEach
    void setUpMapperStubs() {
        when(apnProfileMapper.toEntity(any())).thenReturn(new ApnProfile());
        when(apnProfileMapper.toResponse(any())).thenAnswer(inv -> {
            ApnProfile e = inv.getArgument(0);
            if (e == null) return null;
            ApnProfileResponse r = new ApnProfileResponse();
            r.setId(e.getId());
            r.setDnn(e.getDnn());
            r.setSst(e.getSst());
            r.setEnabled(e.isEnabled());
            r.setStatus(e.getStatus());
            r.setDescription(e.getDescription());
            r.setQos(e.getQos());
            r.setSessionAmbr(e.getSessionAmbr());
            r.setPduSessionType(e.getPduSessionType());
            return r;
        });
        when(apnProfileMapper.toResponseList(any())).thenAnswer(inv -> {
            List<ApnProfile> entities = inv.getArgument(0);
            return entities.stream().map(e -> {
                ApnProfileResponse r = new ApnProfileResponse();
                r.setId(e.getId());
                r.setDnn(e.getDnn());
                r.setSst(e.getSst());
                r.setEnabled(e.isEnabled());
                r.setStatus(e.getStatus());
                r.setDescription(e.getDescription());
                return r;
            }).toList();
        });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201() throws Exception {
        ApnProfile profile = buildProfile();
        when(apnProfileService.create(eq(TENANT), any())).thenReturn(profile);

        mockMvc.perform(post(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dnn").value("internet"))
                .andExpect(jsonPath("$.sst").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_returns200() throws Exception {
        when(apnProfileService.list(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildProfile())));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].dnn").value("internet"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_enabledFilter_returns200() throws Exception {
        when(apnProfileService.listEnabled(TENANT)).thenReturn(List.of(buildProfile()));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_returns200() throws Exception {
        when(apnProfileService.getById(TENANT, "apn-1")).thenReturn(buildProfile());

        mockMvc.perform(get(BASE_URL + "/apn-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Default internet APN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listBySst_returns200() throws Exception {
        when(apnProfileService.listBySst(TENANT, 1)).thenReturn(List.of(buildProfile()));

        mockMvc.perform(get(BASE_URL + "/sst/1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sst").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listByStatus_returns200() throws Exception {
        when(apnProfileService.listByStatus(TENANT, ApnProfile.ProfileStatus.ACTIVE))
                .thenReturn(List.of(buildProfile()));

        mockMvc.perform(get(BASE_URL + "/status/ACTIVE")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns200() throws Exception {
        ApnProfile profile = buildProfile();
        when(apnProfileService.update(eq(TENANT), eq("apn-1"), any())).thenReturn(profile);

        mockMvc.perform(put(BASE_URL + "/apn-1")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/apn-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(apnProfileService).delete(TENANT, "apn-1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deprecate_returns200() throws Exception {
        ApnProfile deprecated = buildProfile();
        deprecated.setStatus(ApnProfile.ProfileStatus.DEPRECATED);
        deprecated.setEnabled(false);
        when(apnProfileService.deprecate(TENANT, "apn-1")).thenReturn(deprecated);

        mockMvc.perform(post(BASE_URL + "/apn-1/deprecate")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPRECATED"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void count_returns200() throws Exception {
        when(apnProfileService.count(TENANT)).thenReturn(5L);

        mockMvc.perform(get(BASE_URL + "/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void list_operatorRole_returns200() throws Exception {
        when(apnProfileService.list(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildProfile())));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk());
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private ApnProfile buildProfile() {
        ApnProfile p = new ApnProfile();
        p.setId("apn-1");
        p.setTenantId(TENANT);
        p.setDnn("internet");
        p.setSst(1);
        p.setPduSessionType(ApnProfile.PduSessionType.IPV4);
        p.setEnabled(true);
        p.setStatus(ApnProfile.ProfileStatus.ACTIVE);
        p.setDescription("Default internet APN");

        ApnProfile.QosProfile qos = new ApnProfile.QosProfile();
        qos.setFiveQi(9);
        qos.setArpPriorityLevel(8);
        p.setQos(qos);

        ApnProfile.Ambr ambr = new ApnProfile.Ambr();
        ambr.setUplinkKbps(1000000L);
        ambr.setDownlinkKbps(2000000L);
        p.setSessionAmbr(ambr);

        return p;
    }
}
