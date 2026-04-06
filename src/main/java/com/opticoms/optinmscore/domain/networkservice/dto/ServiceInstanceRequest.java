package com.opticoms.optinmscore.domain.networkservice.dto;

import com.opticoms.optinmscore.domain.networkservice.model.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ServiceInstanceRequest {

    @NotBlank
    private String name;

    @NotNull
    private ServiceType type;

    private List<String> ipAddresses;

    @Schema(description = "URL used by the health-check scheduler (optional, internal use)",
            example = "http://10.20.2.10:9090/metrics")
    private String healthCheckUrl;
}
