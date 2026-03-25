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
 * Fabric8 Kubernetes Client kullanarak:
 * 1. open5gs namespace'indeki ConfigMap'leri günceller.
 * 2. İlgili Deployment'lara rollout restart uygular.
 *
 * "Desired-state" yaklaşımı:
 * Delta hesabı yapılmaz. Her çağrıda ConfigMap'in tüm içeriği
 * yeni üretilmiş YAML ile tamamen değiştirilir ve pod restart edilir.
 *
 * K8s bağlantısı:
 * - In-cluster (pod içinde): ServiceAccount token otomatik okunur
 * - Local dev: ~/.kube/config veya KUBECONFIG env var
 * - kubernetes.deploy.enabled=false → K8s'e hiç bağlanmaz (dev/test modu)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesDeployService {

    private final KubernetesClient k8sClient;

    // application.yml → kubernetes.namespace (default: open5gs)
    @Value("${kubernetes.namespace:open5gs}")
    private String namespace;

    // application.yml → kubernetes.deploy.enabled
    // false ise K8s'e apply yapılmaz, sadece log yazılır (local dev için)
    @Value("${kubernetes.deploy.enabled:true}")
    private boolean deployEnabled;

    /**
     * Tüm NF'leri deploy eder.
     * deploy/all endpoint'i tarafından çağrılır.
     *
     * @param rendered ConfigRenderService'ten gelen YAML'lar
     * @return Her NF için başarı/başarısızlık bilgisi
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

        // UPF (slice 1) — hem upfcfg.yaml hem wrapper.sh
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
        // Her NF için: <nf>-configmap → <nf>cfg.yaml → open5gs-<nf>
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

    /**
     * Sadece AMF ConfigMap'ini günceller ve AMF'i restart eder.
     */
    public DeployResult applyAmf(RenderedConfigs rendered) {
        List<String> updatedCms = new ArrayList<>();
        List<String> restartedDeps = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        applyConfigMap("amf-configmap", Map.of("amfcfg.yaml", rendered.getAmfYaml()),
                updatedCms, errors);
        restartDeployment("open5gs-amf", restartedDeps, errors);

        return buildResult(updatedCms, restartedDeps, errors);
    }

    /**
     * Sadece SMF ConfigMap'ini günceller ve SMF'i restart eder.
     */
    public DeployResult applySmf(RenderedConfigs rendered) {
        List<String> updatedCms = new ArrayList<>();
        List<String> restartedDeps = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        applyConfigMap("smf1-configmap", Map.of("smfcfg.yaml", rendered.getSmfYaml()),
                updatedCms, errors);
        restartDeployment("open5gs-smf1", restartedDeps, errors);

        return buildResult(updatedCms, restartedDeps, errors);
    }

    /**
     * Sadece UPF ConfigMap'ini günceller ve UPF'i restart eder.
     */
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

    // ── Core metodlar ────────────────────────────────────────────────────────

    /**
     * ConfigMap'i K8s'te günceller.
     *
     * Fabric8 yaklaşımı: önce GET → data alanını değiştir → PATCH/UPDATE.
     * ConfigMap yoksa hata listesine yaz, exception fırlatma (diğer NF'ler devam etsin).
     *
     * @param configMapName  K8s ConfigMap adı (örn. "amf-configmap")
     * @param dataEntries    ConfigMap.data içine yazılacak key-value çiftleri
     * @param updatedCms     Başarılı güncelleme listesi (out param)
     * @param errors         Hata listesi (out param)
     */
    private void applyConfigMap(String configMapName,
                                Map<String, String> dataEntries,
                                List<String> updatedCms,
                                List<String> errors) {
        // K8s deploy kapalıysa (local dev/test): sadece log yaz, gerçek apply yapma
        if (!deployEnabled) {
            log.info("[DRY-RUN] Would update ConfigMap: {}/{}", namespace, configMapName);
            updatedCms.add(configMapName + " (dry-run)");
            return;
        }

        try {
            // 1. Mevcut ConfigMap'i K8s'ten al
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
     * Deployment'a rollout restart uygular.
     *
     * "kubectl rollout restart" ne yapar?
     * Deployment'ın pod template annotation'ına
     * "kubectl.kubernetes.io/restartedAt" = <timestamp> yazar.
     * Bu değişiklik Deployment controller'ını yeni bir ReplicaSet başlatmaya tetikler.
     * Sonuç: tüm pod'lar yeni ConfigMap ile yeniden başlar.
     *
     * @param deploymentName K8s Deployment adı (örn. "open5gs-amf")
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
