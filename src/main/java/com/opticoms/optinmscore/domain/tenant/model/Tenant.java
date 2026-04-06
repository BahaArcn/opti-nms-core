package com.opticoms.optinmscore.domain.tenant.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "tenants")
@CompoundIndexes({
    @CompoundIndex(name = "tenantId_unique", def = "{'tenantId': 1}", unique = true)
})
public class Tenant extends BaseEntity {

    @NotBlank
    private String name;

    @NotBlank
    private String amfUrl;

    @NotBlank
    private String smfUrl;

    private String open5gsMongoUri;

    @Schema(description = "UPF Prometheus metrics endpoint URL (optional)", example = "http://upf:9090/metrics")
    private String upfMetricsUrl;

    @Indexed
    private boolean active = true;
}
