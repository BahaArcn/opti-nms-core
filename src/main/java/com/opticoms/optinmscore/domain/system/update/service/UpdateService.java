package com.opticoms.optinmscore.domain.system.update.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.system.update.dto.UpdateCheckResult;
import com.opticoms.optinmscore.domain.system.update.dto.VersionInfo;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateService {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    private final RestTemplate restTemplate;
    private final KubernetesClient k8sClient;

    @Value("${app.docker-image:opticoms/optinms-core}")
    private String dockerImage;

    @Value("${app.docker-tag:latest}")
    private String dockerTag;

    @Value("${kubernetes.namespace:open5gs}")
    private String namespace;

    @Value("${kubernetes.deploy.enabled:false}")
    private boolean k8sDeployEnabled;

    @Value("${app.update.deployment-name:optinms-core}")
    private String deploymentName;

    public VersionInfo getVersion() {
        VersionInfo info = new VersionInfo();
        info.setAppVersion(buildProperties != null ? buildProperties.getVersion() : "unknown");
        info.setBuildTime(buildProperties != null && buildProperties.getTime() != null
                ? buildProperties.getTime().toString() : "unknown");
        info.setDockerImage(dockerImage + ":" + dockerTag);
        info.setDeployMode(k8sDeployEnabled ? "kubernetes" : "standalone");
        return info;
    }

    @SuppressWarnings("unchecked")
    public UpdateCheckResult checkForUpdate() {
        UpdateCheckResult result = new UpdateCheckResult();
        result.setDockerImage(dockerImage + ":" + dockerTag);
        result.setCheckedAt(Instant.now().toString());

        String buildTime = buildProperties != null && buildProperties.getTime() != null
                ? buildProperties.getTime().toString() : "unknown";
        result.setCurrentBuildTime(buildTime);

        if ("opticoms/optinms-core".equals(dockerImage)) {
            result.setUpdateAvailable(false);
            result.setMessage("Update check not configured: set APP_DOCKER_IMAGE environment variable");
            return result;
        }

        if ("unknown".equals(buildTime)) {
            result.setUpdateAvailable(false);
            result.setMessage("Build time not available");
            return result;
        }

        try {
            String url = "https://hub.docker.com/v2/repositories/" + dockerImage + "/tags/" + dockerTag + "/";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("last_updated")) {
                String hubLastUpdated = (String) response.get("last_updated");
                result.setHubLastUpdated(hubLastUpdated);

                try {
                    Instant hubTime = Instant.parse(hubLastUpdated);
                    Instant localTime = Instant.parse(buildTime);
                    result.setUpdateAvailable(hubTime.isAfter(localTime));
                    result.setMessage(result.isUpdateAvailable()
                            ? "A newer version is available on Docker Hub"
                            : "You are running the latest version");
                } catch (Exception e) {
                    result.setUpdateAvailable(false);
                    result.setMessage("Could not compare versions");
                }
            } else {
                result.setUpdateAvailable(false);
                result.setMessage("Docker Hub response missing last_updated field");
            }
        } catch (Exception e) {
            log.warn("Failed to check Docker Hub for updates: {}", e.getMessage());
            result.setUpdateAvailable(false);
            result.setMessage("Could not reach Docker Hub: " + e.getMessage());
        }

        return result;
    }

    @Audited(action = AuditAction.APPLY, entityType = "SystemUpdate")
    public Map<String, String> applyUpdate() {
        if (!k8sDeployEnabled) {
            return Map.of(
                    "status", "NOT_SUPPORTED",
                    "message", "Auto-update requires K8S_DEPLOY_ENABLED=true"
            );
        }

        try {
            k8sClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .rolling().restart();

            log.info("Rolling restart triggered for deployment: {}", deploymentName);
            return Map.of(
                    "status", "TRIGGERED",
                    "message", "Rolling restart triggered for: " + deploymentName
            );
        } catch (Exception e) {
            log.error("Failed to trigger rolling restart for {}: {}", deploymentName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to trigger rolling restart");
        }
    }
}
