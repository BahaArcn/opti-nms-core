package com.opticoms.optinmscore.domain.networkservice.dto;

import com.opticoms.optinmscore.domain.networkservice.model.ServiceStatus;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceType;
import lombok.Data;

import java.util.List;

@Data
public class ServiceInstanceResponse {

    private String id;
    private String networkId;
    private String name;
    private ServiceType type;
    private List<String> ipAddresses;
    private String healthCheckUrl;
    private ServiceStatus status;
    private String statusMessage;
    private Long lastHealthCheck;
    private Long createdAt;
    private Long updatedAt;
}
