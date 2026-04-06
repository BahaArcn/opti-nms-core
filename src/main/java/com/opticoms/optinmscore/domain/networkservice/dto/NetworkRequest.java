package com.opticoms.optinmscore.domain.networkservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NetworkRequest {

    @NotBlank
    private String name;

    private String description;
}
