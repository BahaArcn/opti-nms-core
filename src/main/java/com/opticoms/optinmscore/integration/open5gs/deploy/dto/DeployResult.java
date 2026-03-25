package com.opticoms.optinmscore.integration.open5gs.deploy.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Deploy işleminin sonucunu özetleyen yanıt DTO'su.
 *
 * Controller bu nesneyi JSON olarak döndürür.
 * Frontend hangi ConfigMap'in güncellendiğini, hangi pod'un restart edildiğini
 * ve varsa hataları bu nesneden okur.
 */
@Getter
@Builder
public class DeployResult {

    // Deploy başarıyla tamamlandı mı? (kısmi başarı = false)
    private final boolean success;

    // Deploy'un tamamlandığı zaman damgası
    private final Instant deployedAt;

    // Güncellenen ConfigMap isimleri (örn. "amf-configmap", "smf1-configmap")
    private final List<String> updatedConfigMaps;

    // Restart edilen Deployment isimleri (örn. "open5gs-amf", "open5gs-smf1")
    private final List<String> restartedDeployments;

    // Başarısız olan işlemler ve sebebleri
    // Boş liste = hata yok
    private final List<String> errors;

    // Kaç NF başarıyla deploy edildi
    private final int successCount;

    // Kaç NF deploy edilemedi
    private final int failureCount;

    // K8S_DEPLOY_ENABLED=false ise true — gerçek K8s işlemi yapılmadı
    private final boolean dryRun;
}
