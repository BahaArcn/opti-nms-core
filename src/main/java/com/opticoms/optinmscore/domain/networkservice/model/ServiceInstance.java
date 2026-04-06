package com.opticoms.optinmscore.domain.networkservice.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "service_instances")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_network_idx", def = "{'tenantId': 1, 'networkId': 1}"),
        @CompoundIndex(name = "tenant_network_name_idx", def = "{'tenantId': 1, 'networkId': 1, 'name': 1}", unique = true)
})
@Schema(description = "A managed service instance within a network (e.g. Control Plane, Data Plane)")
public class ServiceInstance extends BaseEntity {

    @NotBlank
    @Schema(description = "ID of the parent Network", example = "6629a1b...")
    private String networkId;

    @NotBlank
    @Schema(description = "Service display name", example = "CONTROL PLANE")
    private String name;

    @NotNull
    @Schema(description = "Service category")
    private ServiceType type;

    @Schema(description = "IP addresses associated with this service instance")
    private List<String> ipAddresses;

    @Schema(description = "URL used by the health-check scheduler (internal)")
    private String healthCheckUrl;

    @Schema(description = "Current service status")
    private ServiceStatus status = ServiceStatus.UNKNOWN;

    @Schema(description = "Human-readable status message", example = "The service is running.")
    private String statusMessage;

    @Schema(description = "Last health check timestamp (epoch ms)")
    private Long lastHealthCheck;
}
