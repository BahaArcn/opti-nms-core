package com.opticoms.optinmscore.integration.open5gs.deploy.controller;

import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.integration.open5gs.deploy.dto.DeployResult;
import com.opticoms.optinmscore.integration.open5gs.deploy.dto.RenderedConfigs;
import com.opticoms.optinmscore.integration.open5gs.deploy.service.ConfigRenderService;
import com.opticoms.optinmscore.integration.open5gs.deploy.service.KubernetesDeployService;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Open5gsDeployController.class)
@AutoConfigureMockMvc(addFilters = false)
class Open5gsDeployControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String BASE = "/api/v1/open5gs/deploy";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConfigRenderService renderService;
    @MockBean
    private KubernetesDeployService deployService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private static DeployResult sampleDeployResult() {
        return DeployResult.builder()
                .success(true)
                .deployedAt(Instant.parse("2026-04-06T12:00:00Z"))
                .updatedConfigMaps(List.of("amf-configmap"))
                .restartedDeployments(List.of("open5gs-amf"))
                .errors(List.of())
                .successCount(1)
                .failureCount(0)
                .dryRun(false)
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deployAll_returns200() throws Exception {
        RenderedConfigs rendered = RenderedConfigs.builder().amfYaml("a").build();
        when(renderService.renderAll(TENANT)).thenReturn(rendered);
        when(deployService.applyAll(rendered)).thenReturn(sampleDeployResult());

        mockMvc.perform(post(BASE + "/all").requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.dryRun").value(false))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.updatedConfigMaps[0]").value("amf-configmap"));

        verify(renderService).renderAll(TENANT);
        verify(deployService).applyAll(rendered);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deployAmf_returns200() throws Exception {
        RenderedConfigs rendered = RenderedConfigs.builder().amfYaml("amf").build();
        when(renderService.renderAmfOnly(TENANT)).thenReturn(rendered);
        when(deployService.applyAmf(rendered)).thenReturn(sampleDeployResult());

        mockMvc.perform(post(BASE + "/amf").requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(renderService).renderAmfOnly(TENANT);
        verify(deployService).applyAmf(rendered);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deploySmf_returns200() throws Exception {
        RenderedConfigs rendered = RenderedConfigs.builder().smfYaml("smf").build();
        when(renderService.renderSmfOnly(TENANT)).thenReturn(rendered);
        when(deployService.applySmf(rendered)).thenReturn(sampleDeployResult());

        mockMvc.perform(post(BASE + "/smf").requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(renderService).renderSmfOnly(TENANT);
        verify(deployService).applySmf(rendered);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deployUpf_returns200() throws Exception {
        RenderedConfigs rendered = RenderedConfigs.builder().upfYaml("upf").build();
        when(renderService.renderUpfOnly(TENANT)).thenReturn(rendered);
        when(deployService.applyUpf(rendered)).thenReturn(sampleDeployResult());

        mockMvc.perform(post(BASE + "/upf").requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(renderService).renderUpfOnly(TENANT);
        verify(deployService).applyUpf(rendered);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void preview_returns200_withCoreKeys_andOptionalNfs() throws Exception {
        Map<String, String> common = new LinkedHashMap<>();
        common.put("ausf", "ausf-yaml-body");

        RenderedConfigs rendered = RenderedConfigs.builder()
                .amfYaml("amf-body")
                .smfYaml("smf-body")
                .upfYaml("upf-body")
                .wrapperSh("#!/bin/sh")
                .nrfYaml("nrf-body")
                .nssfYaml("nssf-body")
                .mmeYaml("mme-body")
                .commonNfYamls(common)
                .build();

        when(renderService.renderAll(TENANT)).thenReturn(rendered);

        mockMvc.perform(get(BASE + "/preview").requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['amfcfg.yaml']").value("amf-body"))
                .andExpect(jsonPath("$.['smfcfg.yaml']").value("smf-body"))
                .andExpect(jsonPath("$.['upfcfg.yaml']").value("upf-body"))
                .andExpect(jsonPath("$.['wrapper.sh']").value("#!/bin/sh"))
                .andExpect(jsonPath("$.['nrfcfg.yaml']").value("nrf-body"))
                .andExpect(jsonPath("$.['nssfcfg.yaml']").value("nssf-body"))
                .andExpect(jsonPath("$.['ausfcfg.yaml']").value("ausf-yaml-body"))
                .andExpect(jsonPath("$.['mmecfg.yaml']").value("mme-body"));

        verify(renderService).renderAll(TENANT);
        verify(deployService, never()).applyAll(any());
    }
}
