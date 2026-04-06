package com.opticoms.optinmscore.domain.edgelocation.dto;

import com.opticoms.optinmscore.domain.edgelocation.model.EdgeLocation;
import lombok.Data;

@Data
public class EdgeLocationResponse {
    private String id;
    private String name;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private EdgeLocation.EdgeLocationStatus status;
    private Long createdAt;
    private Long updatedAt;
}
