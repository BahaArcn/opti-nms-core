package com.opticoms.optinmscore.domain.networkservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

        @Schema(description = "Aggregate status: RUNNING, DEGRADED, ERROR, or UNKNOWN", example = "RUNNING")
        private String status;

        @Schema(description = "Status detail message", example = "All 9 services running.")
        private String statusMessage;

        @Schema(description = "Individual NF statuses within this group")
        private List<NfStatus> components;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NfStatus {
        @Schema(description = "Network Function name", example = "AMF")
        private String name;

        @Schema(description = "NF status: RUNNING, ERROR, or UNKNOWN", example = "RUNNING")
        private String status;

        @Schema(description = "IP address parsed from the NF URL", example = "10.244.0.129")
        private String ipAddress;
    }
}
