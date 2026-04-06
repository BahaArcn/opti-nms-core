package com.opticoms.optinmscore.integration.open5gs.deploy.service;

import com.opticoms.optinmscore.integration.open5gs.deploy.dto.DeployResult;
import com.opticoms.optinmscore.integration.open5gs.deploy.dto.RenderedConfigs;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Applies rendered YAML to the cluster using the Fabric8 {@link KubernetesClient}:
 * 1. Update ConfigMaps in the configured namespace (default {@code open5gs}).
 * 2. Roll out restarted Deployments.
 *
 * Desired-state: no delta merge; each call replaces ConfigMap data with freshly rendered YAML and restarts pods.
 *
 * Connectivity: in-cluster ServiceAccount, or local {@code ~/.kube/config} / {@code KUBECONFIG}.
 * When {@code kubernetes.deploy.enabled=false}, no API calls are made (dev/test).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesDeployService {

    private final KubernetesClient k8sClient;

    // application.yml → kubernetes.namespace (default: open5gs)
    @Value("${kubernetes.namespace:open5gs}")
    private String namespace;

    // application.yml → kubernetes.deploy.enabled (false: log only, no cluster apply)
    @Value("${kubernetes.deploy.enabled:true}")
    private boolean deployEnabled;

    /**
     * Applies all NFs (used by {@code /deploy/all}).
     *
     * @param rendered output from {@link com.opticoms.optinmscore.integration.open5gs.deploy.service.ConfigRenderService}
     */
    public DeployResult applyAll(RenderedConfigs rendered) {
        List<String> updatedCms = new ArrayList<>();
        List<String> restartedDeps = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // AMF
        applyConfigMap("amf-configmap", Map.of("amfcfg.yaml", rendered.getAmfYaml()),
                updatedCms, errors);
        restartDeployment("open5gs-amf", restartedDeps, errors);

        // SMF (slice 1)
        applyConfigMap("smf1-configmap", Map.of("smfcfg.yaml", rendered.getSmfYaml()),
                updatedCms, errors);
        restartDeployment("open5gs-smf1", restartedDeps, errors);

        // UPF (slice 1): upfcfg.yaml + wrapper.sh
        applyConfigMap("upf1-configmap",
                Map.of("upfcfg.yaml", rendered.getUpfYaml(),
                       "wrapper.sh",  rendered.getWrapperSh()),
                updatedCms, errors);
        restartDeployment("open5gs-upf1", restartedDeps, errors);

        // NRF
        applyConfigMap("nrf-configmap", Map.of("nrfcfg.yaml", rendered.getNrfYaml()),
                updatedCms, errors);
        restartDeployment("open5gs-nrf", restartedDeps, errors);

        // NSSF
        applyConfigMap("nssf-configmap", Map.of("nssfcfg.yaml", rendered.getNssfYaml()),
                updatedCms, errors);
        restartDeployment("open5gs-nssf", restartedDeps, errors);

        // Common NFs: ausf / udm / udr / bsf / pcf / scp
        if (rendered.getCommonNfYamls() != null) {
            for (Map.Entry<String, String> entry : rendered.getCommonNfYamls().entrySet()) {
                String nf = entry.getKey();
                String yamlContent = entry.getValue();
                applyConfigMap(nf + "-configmap",
                        Map.of(nf + "cfg.yaml", yamlContent),
                        updatedCms, errors);
                restartDeployment("open5gs-" + nf, restartedDeps, errors);
            }
        }

        if (rendered.getMmeYaml() != null) {
            applyConfigMap("mme-configmap", Map.of("mmecfg.yaml", rendered.getMmeYaml()),
                    updatedCms, errors);
            restartDeployment("open5gs-mme", restartedDeps, errors);
        }

        if (rendered.getSgwuYaml() != null) {
            applyConfigMap("sgwu-configmap", Map.of("sgwucfg.yaml", rendered.getSgwuYaml()),
                    updatedCms, errors);
            restartDeployment("open5gs-sgwu", restartedDeps, errors);
        }

        return buildResult(updatedCms, restartedDeps, errors);
    }

    /** Updates the AMF ConfigMap and restarts the AMF Deployment. */
    public DeployResult applyAmf(RenderedConfigs rendered) {
        List<String> updatedCms = new ArrayList<>();
        List<String> restartedDeps = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        applyConfigMap("amf-configmap", Map.of("amfcfg.yaml", rendered.getAmfYaml()),
                updatedCms, errors);
        restartDeployment("open5gs-amf", restartedDeps, errors);

        return buildResult(updatedCms, restartedDeps, errors);
    }

    /** Updates the SMF ConfigMap and restarts the SMF Deployment. */
    public DeployResult applySmf(RenderedConfigs rendered) {
        List<String> updatedCms = new ArrayList<>();
        List<String> restartedDeps = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        applyConfigMap("smf1-configmap", Map.of("smfcfg.yaml", rendered.getSmfYaml()),
                updatedCms, errors);
        restartDeployment("open5gs-smf1", restartedDeps, errors);

        return buildResult(updatedCms, restartedDeps, errors);
    }

    /** Updates the UPF ConfigMap and restarts the UPF Deployment. */
    public DeployResult applyUpf(RenderedConfigs rendered) {
        List<String> updatedCms = new ArrayList<>();
        List<String> restartedDeps = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        applyConfigMap("upf1-configmap",
                Map.of("upfcfg.yaml", rendered.getUpfYaml(),
                       "wrapper.sh",  rendered.getWrapperSh()),
                updatedCms, errors);
        restartDeployment("open5gs-upf1", restartedDeps, errors);

        return buildResult(updatedCms, restartedDeps, errors);
    }

    /**
     * Updates or creates a ConfigMap (GET → merge {@code data} → update).
     * Missing ConfigMap: record error and continue with other NFs (no thrown exception).
     *
     * @param configMapName  e.g. {@code amf-configmap}
     * @param dataEntries    keys/values for {@code ConfigMap.data}
     * @param updatedCms     successful updates (output)
     * @param errors         failures (output)
     */
    private void applyConfigMap(String configMapName,
                                Map<String, String> dataEntries,
                                List<String> updatedCms,
                                List<String> errors) {
        if (!deployEnabled) {
            log.info("[DRY-RUN] Would update ConfigMap: {}/{}", namespace, configMapName);
            updatedCms.add(configMapName + " (dry-run)");
            return;
        }

        try {
            ConfigMap existing = k8sClient.configMaps()
                    .inNamespace(namespace)
                    .withName(configMapName)
                    .get();

            if (existing == null) {
                log.info("ConfigMap not found, creating: {}/{}", namespace, configMapName);
                ConfigMap newCm = new ConfigMapBuilder()
                        .withNewMetadata()
                            .withName(configMapName)
                            .withNamespace(namespace)
                        .endMetadata()
                        .withData(new java.util.HashMap<>(dataEntries))
                        .build();
                k8sClient.configMaps()
                        .inNamespace(namespace)
                        .resource(newCm)
                        .create();
            } else {
                Map<String, String> data = existing.getData();
                if (data == null) data = new java.util.HashMap<>();
                data.putAll(dataEntries);
                existing.setData(data);

                k8sClient.configMaps()
                        .inNamespace(namespace)
                        .resource(existing)
                        .update();
            }

            log.info("ConfigMap updated: {}/{}", namespace, configMapName);
            updatedCms.add(configMapName);

        } catch (Exception e) {
            String msg = "Failed to update ConfigMap " + configMapName + ": " + e.getMessage();
            log.error(msg, e);
            errors.add(msg);
        }
    }

    /**
     * Triggers a rollout restart (same idea as {@code kubectl rollout restart}):
     * sets pod-template annotation {@code kubectl.kubernetes.io/restartedAt}, which starts a new ReplicaSet
     * so pods reload mounted ConfigMap data.
     *
     * @param deploymentName e.g. {@code open5gs-amf}
     */
    private void restartDeployment(String deploymentName,
                                   List<String> restartedDeps,
                                   List<String> errors) {
        if (!deployEnabled) {
            log.info("[DRY-RUN] Would restart Deployment: {}/{}", namespace, deploymentName);
            restartedDeps.add(deploymentName + " (dry-run)");
            return;
        }

        try {
            k8sClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .rolling()
                    .restart();

            log.info("Deployment restarted: {}/{}", namespace, deploymentName);
            restartedDeps.add(deploymentName);

        } catch (Exception e) {
            String msg = "Failed to restart Deployment " + deploymentName + ": " + e.getMessage();
            log.error(msg, e);
            errors.add(msg);
        }
    }

    private DeployResult buildResult(List<String> updatedCms,
                                     List<String> restartedDeps,
                                     List<String> errors) {
        return DeployResult.builder()
                .success(errors.isEmpty())
                .deployedAt(Instant.now())
                .updatedConfigMaps(updatedCms)
                .restartedDeployments(restartedDeps)
                .errors(errors)
                .successCount(restartedDeps.size())
                .failureCount(errors.size())
                .dryRun(!deployEnabled)
                .build();
    }
}
