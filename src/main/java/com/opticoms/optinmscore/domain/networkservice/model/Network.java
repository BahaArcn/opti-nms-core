package com.opticoms.optinmscore.domain.networkservice.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "networks")
@CompoundIndex(name = "tenant_name_idx", def = "{'tenantId': 1, 'name': 1}", unique = true)
@Schema(description = "Logical network grouping of service instances")
public class Network extends BaseEntity {

    @NotBlank
    @Schema(description = "Unique network name within tenant", example = "Default Network")
    private String name;

    @Schema(description = "Human-readable description", example = "Primary production network")
    private String description;

    @Schema(description = "Network status")
    private NetworkStatus status = NetworkStatus.ACTIVE;
}
