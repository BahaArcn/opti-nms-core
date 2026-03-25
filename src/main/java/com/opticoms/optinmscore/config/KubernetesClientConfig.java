package com.opticoms.optinmscore.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fabric8 KubernetesClient'ı Spring Bean olarak kaydeder.
 *
 * Otomatik konfigürasyon öncelik sırası:
 * 1. In-cluster (pod içinde çalışıyorsa): /var/run/secrets/kubernetes.io/serviceaccount/
 *    → ServiceAccount token + CA cert otomatik okunur.
 * 2. Local geliştirme: ~/.kube/config dosyası kullanılır.
 * 3. KUBECONFIG env var ile override edilebilir.
 *
 * KubernetesDeployService bu bean'i inject eder.
 */
@Configuration
public class KubernetesClientConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        // KubernetesClientBuilder: ortamı otomatik algılar (in-cluster vs local)
        return new KubernetesClientBuilder().build();
    }
}
