package com.opticoms.optinmscore.domain.suci.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.suci.dto.SuciProfileResponse;
import com.opticoms.optinmscore.domain.suci.mapper.SuciProfileMapper;
import com.opticoms.optinmscore.domain.suci.model.SuciProfile;
import com.opticoms.optinmscore.domain.suci.service.SuciProfileService;
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

@WebMvcTest(SuciProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class SuciProfileControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE_URL = "/api/v1/suci/profiles";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SuciProfileService suciProfileService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private SuciProfileMapper suciProfileMapper;

    @BeforeEach
    void setUpMapperStubs() {
        when(suciProfileMapper.toEntity(any())).thenReturn(new SuciProfile());
        when(suciProfileMapper.toResponse(any())).thenAnswer(inv -> {
            SuciProfile e = inv.getArgument(0);
            if (e == null) return null;
            SuciProfileResponse r = new SuciProfileResponse();
            r.setId(e.getId());
            r.setProtectionScheme(e.getProtectionScheme());
            r.setHomeNetworkPublicKeyId(e.getHomeNetworkPublicKeyId());
            r.setHomeNetworkPublicKey(e.getHomeNetworkPublicKey());
            r.setKeyStatus(e.getKeyStatus());
            r.setDescription(e.getDescription());
            return r;
        });
        when(suciProfileMapper.toResponseList(any())).thenAnswer(inv -> {
            List<SuciProfile> entities = inv.getArgument(0);
            return entities.stream().map(e -> {
                SuciProfileResponse r = new SuciProfileResponse();
                r.setId(e.getId());
                r.setProtectionScheme(e.getProtectionScheme());
                r.setHomeNetworkPublicKeyId(e.getHomeNetworkPublicKeyId());
                r.setKeyStatus(e.getKeyStatus());
                r.setDescription(e.getDescription());
                return r;
            }).toList();
        });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201() throws Exception {
        SuciProfile profile = buildProfile();
        when(suciProfileService.create(eq(TENANT), any())).thenReturn(profile);

        mockMvc.perform(post(BASE_URL)
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protectionScheme").value("PROFILE_A"))
                .andExpect(jsonPath("$.homeNetworkPublicKeyId").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_returns200() throws Exception {
        when(suciProfileService.list(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildProfile())));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].protectionScheme").value("PROFILE_A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_returns200() throws Exception {
        when(suciProfileService.getById(TENANT, "suci-1")).thenReturn(buildProfile());

        mockMvc.perform(get(BASE_URL + "/suci-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Test HNET key"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listByScheme_returns200() throws Exception {
        when(suciProfileService.listByScheme(TENANT, SuciProfile.ProtectionScheme.PROFILE_A))
                .thenReturn(List.of(buildProfile()));

        mockMvc.perform(get(BASE_URL + "/scheme/PROFILE_A")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].protectionScheme").value("PROFILE_A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listByStatus_returns200() throws Exception {
        when(suciProfileService.listByStatus(TENANT, SuciProfile.KeyStatus.ACTIVE))
                .thenReturn(List.of(buildProfile()));

        mockMvc.perform(get(BASE_URL + "/status/ACTIVE")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns200() throws Exception {
        SuciProfile profile = buildProfile();
        when(suciProfileService.update(eq(TENANT), eq("suci-1"), any())).thenReturn(profile);

        mockMvc.perform(put(BASE_URL + "/suci-1")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/suci-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(suciProfileService).delete(TENANT, "suci-1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revoke_returns200() throws Exception {
        SuciProfile revoked = buildProfile();
        revoked.setKeyStatus(SuciProfile.KeyStatus.REVOKED);
        when(suciProfileService.revokeKey(TENANT, "suci-1")).thenReturn(revoked);

        mockMvc.perform(post(BASE_URL + "/suci-1/revoke")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyStatus").value("REVOKED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void count_returns200() throws Exception {
        when(suciProfileService.count(TENANT)).thenReturn(7L);

        mockMvc.perform(get(BASE_URL + "/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void list_operatorRole_returns200() throws Exception {
        when(suciProfileService.list(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildProfile())));

        mockMvc.perform(get(BASE_URL)
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk());
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private SuciProfile buildProfile() {
        SuciProfile p = new SuciProfile();
        p.setId("suci-1");
        p.setTenantId(TENANT);
        p.setProtectionScheme(SuciProfile.ProtectionScheme.PROFILE_A);
        p.setHomeNetworkPublicKeyId(1);
        p.setHomeNetworkPublicKey(
                "5a8d38864820197c3394b92613b20b91633cbd897119273bf8e4a6f4eec0a650");
        p.setHomeNetworkPrivateKey(
                "c53c2208b4d1b100f0599b5b9856856fe665df1d2eab0978000b83e8fb721b4a");
        p.setKeyStatus(SuciProfile.KeyStatus.ACTIVE);
        p.setDescription("Test HNET key");
        return p;
    }
}
