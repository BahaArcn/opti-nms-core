package com.opticoms.optinmscore.integration.open5gs.deploy.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Her NF için üretilmiş YAML string'lerini taşır.
 *
 * Renderer katmanından çıkıp KubernetesDeployService'e girer.
 * Builder pattern: sadece ilgili NF'ler için üretim yapılabilsin diye
 * (örneğin deploy/amf çağrısında sadece amfYaml dolu olur).
 */
@Getter
@Builder
public class RenderedConfigs {

    // amf-configmap içindeki "amfcfg.yaml" key'inin değeri
    private final String amfYaml;

    // smf1-configmap içindeki "smfcfg.yaml" key'inin değeri
    private final String smfYaml;

    // upf1-configmap içindeki "upfcfg.yaml" key'inin değeri
    private final String upfYaml;

    // upf1-configmap içindeki "wrapper.sh" key'inin değeri
    // UPF'e özel: tun interface + iptables kurulumu için init script
    private final String wrapperSh;

    // nrf-configmap içindeki "nrfcfg.yaml" key'inin değeri
    private final String nrfYaml;

    // nssf-configmap içindeki "nssfcfg.yaml" key'inin değeri
    private final String nssfYaml;

    // mme-configmap içindeki "mmecfg.yaml" key'inin değeri (4G ve HYBRID mod)
    private final String mmeYaml;

    // sgwu-configmap içindeki "sgwucfg.yaml" key'inin değeri (4G ve HYBRID mod)
    private final String sgwuYaml;

    // ausf/udm/udr/bsf vb. için ortak YAML (şimdilik sadece global.max.ue içerir)
    // Key: NF adı (örn. "ausf"), Value: YAML string
    private final java.util.Map<String, String> commonNfYamls;
}
