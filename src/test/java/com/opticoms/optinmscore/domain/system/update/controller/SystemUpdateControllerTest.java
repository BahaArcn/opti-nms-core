package com.opticoms.optinmscore.domain.system.update.controller;

import com.opticoms.optinmscore.domain.system.update.dto.UpdateCheckResult;
import com.opticoms.optinmscore.domain.system.update.dto.VersionInfo;
import com.opticoms.optinmscore.domain.system.update.service.UpdateService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemUpdateController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemUpdateControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UpdateService updateService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /version - returns 200")
    @WithMockUser(roles = "ADMIN")
    void getVersion_returns200() throws Exception {
        VersionInfo info = new VersionInfo();
        info.setAppVersion("1.0.0");
        info.setBuildTime("2026-01-01T00:00:00Z");
        info.setDockerImage("opticoms/optinms-core:latest");
        info.setDeployMode("standalone");
        when(updateService.getVersion()).thenReturn(info);

        mockMvc.perform(get("/api/v1/system/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appVersion").value("1.0.0"))
                .andExpect(jsonPath("$.deployMode").value("standalone"));
    }

    @Test
    @DisplayName("POST /update/check - returns 200")
    @WithMockUser(roles = "ADMIN")
    void checkUpdate_returns200() throws Exception {
        UpdateCheckResult result = new UpdateCheckResult();
        result.setUpdateAvailable(false);
        result.setMessage("Up to date");
        result.setDockerImage("opticoms/optinms-core:latest");
        result.setCheckedAt("2026-01-01T00:00:00Z");
        result.setCurrentBuildTime("2026-01-01T00:00:00Z");
        when(updateService.checkForUpdate()).thenReturn(result);

        mockMvc.perform(post("/api/v1/system/update/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updateAvailable").value(false))
                .andExpect(jsonPath("$.message").value("Up to date"));
    }

    @Test
    @DisplayName("POST /update/apply - returns 200")
    @WithMockUser(roles = "ADMIN")
    void applyUpdate_returns200() throws Exception {
        when(updateService.applyUpdate())
                .thenReturn(Map.of("status", "NOT_SUPPORTED", "message", "Auto-update requires K8S_DEPLOY_ENABLED=true"));

        mockMvc.perform(post("/api/v1/system/update/apply"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_SUPPORTED"));
    }
}
