package com.opticoms.optinmscore.domain.system.update.service;

import com.opticoms.optinmscore.domain.system.update.dto.UpdateCheckResult;
import com.opticoms.optinmscore.domain.system.update.dto.VersionInfo;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private KubernetesClient k8sClient;

    @InjectMocks private UpdateService updateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(updateService, "dockerImage", "opticoms/optinms-core");
        ReflectionTestUtils.setField(updateService, "dockerTag", "latest");
        ReflectionTestUtils.setField(updateService, "namespace", "open5gs");
        ReflectionTestUtils.setField(updateService, "deploymentName", "optinms-core");
        ReflectionTestUtils.setField(updateService, "k8sDeployEnabled", false);
    }

    @Test
    @DisplayName("getVersion - when BuildProperties null, returns unknown")
    void getVersion_whenBuildPropertiesNull_returnsUnknown() {
        VersionInfo info = updateService.getVersion();

        assertThat(info.getAppVersion()).isEqualTo("unknown");
        assertThat(info.getBuildTime()).isEqualTo("unknown");
        assertThat(info.getDockerImage()).isEqualTo("opticoms/optinms-core:latest");
        assertThat(info.getDeployMode()).isEqualTo("standalone");
    }

    @Test
    @DisplayName("getVersion - when BuildProperties present, returns real values")
    void getVersion_whenBuildPropertiesPresent_returnsRealValues() {
        BuildProperties bp = mock(BuildProperties.class);
        when(bp.getVersion()).thenReturn("1.0.0");
        when(bp.getTime()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(updateService, "buildProperties", bp);

        VersionInfo info = updateService.getVersion();

        assertThat(info.getAppVersion()).isEqualTo("1.0.0");
        assertThat(info.getBuildTime()).isEqualTo("2026-01-01T00:00:00Z");
    }

    @Test
    @DisplayName("getVersion - when k8sDeployEnabled, deployMode is kubernetes")
    void getVersion_whenK8sEnabled_deployModeKubernetes() {
        ReflectionTestUtils.setField(updateService, "k8sDeployEnabled", true);

        VersionInfo info = updateService.getVersion();

        assertThat(info.getDeployMode()).isEqualTo("kubernetes");
    }

    @Test
    @DisplayName("checkForUpdate - when default image placeholder, returns early with not-configured message")
    void checkForUpdate_whenDefaultImage_returnsNotConfigured() {
        UpdateCheckResult result = updateService.checkForUpdate();

        assertThat(result.isUpdateAvailable()).isFalse();
        assertThat(result.getMessage()).contains("Update check not configured");
        assertThat(result.getMessage()).contains("APP_DOCKER_IMAGE");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("checkForUpdate - when hub returns newer date, returns updateAvailable")
    @SuppressWarnings("unchecked")
    void checkForUpdate_whenHubReturnsNewerDate_returnsUpdateAvailable() {
        ReflectionTestUtils.setField(updateService, "dockerImage", "myregistry/optinms-core");
        BuildProperties bp = mock(BuildProperties.class);
        Instant buildTime = Instant.now().minusSeconds(3600);
        when(bp.getTime()).thenReturn(buildTime);
        ReflectionTestUtils.setField(updateService, "buildProperties", bp);

        String hubUpdated = Instant.now().plusSeconds(3600).toString();
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("last_updated", hubUpdated));

        UpdateCheckResult result = updateService.checkForUpdate();

        assertThat(result.isUpdateAvailable()).isTrue();
        assertThat(result.getHubLastUpdated()).isEqualTo(hubUpdated);
        assertThat(result.getMessage()).contains("newer version");
    }

    @Test
    @DisplayName("checkForUpdate - when buildTime unknown, returns no update")
    void checkForUpdate_whenBuildTimeUnknown_returnsNoUpdate() {
        ReflectionTestUtils.setField(updateService, "dockerImage", "myregistry/optinms-core");

        UpdateCheckResult result = updateService.checkForUpdate();

        assertThat(result.isUpdateAvailable()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Build time not available");
    }

    @Test
    @DisplayName("checkForUpdate - when hub call fails, returns no update with message")
    @SuppressWarnings("unchecked")
    void checkForUpdate_whenHubCallFails_returnsNoUpdateWithMessage() {
        ReflectionTestUtils.setField(updateService, "dockerImage", "myregistry/optinms-core");
        BuildProperties bp = mock(BuildProperties.class);
        when(bp.getTime()).thenReturn(Instant.now());
        ReflectionTestUtils.setField(updateService, "buildProperties", bp);

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        UpdateCheckResult result = updateService.checkForUpdate();

        assertThat(result.isUpdateAvailable()).isFalse();
        assertThat(result.getMessage()).contains("Could not reach Docker Hub");
    }

    @Test
    @DisplayName("applyUpdate - when K8s disabled, returns NOT_SUPPORTED")
    void applyUpdate_whenK8sDisabled_returnsNotSupported() {
        Map<String, String> result = updateService.applyUpdate();

        assertThat(result.get("status")).isEqualTo("NOT_SUPPORTED");
        assertThat(result.get("message")).contains("K8S_DEPLOY_ENABLED");
        verify(k8sClient, never()).apps();
    }
}
