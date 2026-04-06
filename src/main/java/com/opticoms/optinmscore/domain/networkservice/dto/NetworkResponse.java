package com.opticoms.optinmscore.domain.networkservice.dto;

import com.opticoms.optinmscore.domain.networkservice.model.NetworkStatus;
import lombok.Data;

@Data
public class NetworkResponse {

    private String id;
    private String name;
    private String description;
    private NetworkStatus status;
    private Long createdAt;
    private Long updatedAt;
}
