package com.opticoms.optinmscore.integration.open5gs.deploy.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Summary of a deploy operation returned as JSON to the client.
 */
@Getter
@Builder
public class DeployResult {

    private final boolean success;
    private final Instant deployedAt;
    private final List<String> updatedConfigMaps;
    private final List<String> restartedDeployments;
    private final List<String> errors;
    private final int successCount;
    private final int failureCount;

    /** True when {@code K8S_DEPLOY_ENABLED=false} (no cluster changes). */
    private final boolean dryRun;
}
