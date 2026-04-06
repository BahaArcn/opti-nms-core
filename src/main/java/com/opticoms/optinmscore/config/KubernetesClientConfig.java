package com.opticoms.optinmscore.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Fabric8 {@link KubernetesClient} as a Spring bean.
 *
 * Resolution order:
 * 1. In-cluster: reads ServiceAccount token and CA from
 *    {@code /var/run/secrets/kubernetes.io/serviceaccount/}
 * 2. Local: {@code ~/.kube/config}
 * 3. Override with the {@code KUBECONFIG} environment variable
 *
 * Injected by {@code KubernetesDeployService}.
 */
@Configuration
public class KubernetesClientConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
