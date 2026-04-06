package com.opticoms.optinmscore.domain.edgelocation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EdgeLocationRequest {
    @NotBlank
    private String name;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
}
