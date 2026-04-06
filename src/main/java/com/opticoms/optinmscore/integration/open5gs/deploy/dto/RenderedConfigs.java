package com.opticoms.optinmscore.integration.open5gs.deploy.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Holds rendered YAML strings per network function, produced by the renderer layer
 * and consumed by {@link com.opticoms.optinmscore.integration.open5gs.deploy.service.KubernetesDeployService}.
 * The builder allows partial results (e.g. AMF-only deploy leaves other fields null).
 */
@Getter
@Builder
public class RenderedConfigs {

    private final String amfYaml;
    private final String smfYaml;
    private final String upfYaml;
    private final String wrapperSh;
    private final String nrfYaml;
    private final String nssfYaml;
    private final String mmeYaml;
    private final String sgwuYaml;

    /** Keys: NF short name (e.g. {@code ausf}); values: YAML body (today mostly {@code global.max.ue}). */
    private final java.util.Map<String, String> commonNfYamls;
}
