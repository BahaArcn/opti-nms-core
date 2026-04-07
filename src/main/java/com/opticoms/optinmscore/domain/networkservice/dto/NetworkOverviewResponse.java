package com.opticoms.optinmscore.domain.networkservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Network services overview derived from tenant configuration and live health checks")
public class NetworkOverviewResponse {

    private List<ServiceStatusItem> services;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStatusItem {
        @Schema(description = "Human-readable service name", example = "Control Plane")
        private String name;

        @Schema(description = "Service type identifier", example = "CONTROL_PLANE")
        private String type;

        @Schema(description = "Current status: RUNNING, ERROR, or UNKNOWN", example = "RUNNING")
        private String status;

        @Schema(description = "Status detail message", example = "The service is running.")
        private String statusMessage;

        @Schema(description = "IP addresses parsed from the service URL", example = "[\"10.244.0.129\"]")
        private List<String> ipAddresses;
    }
}
