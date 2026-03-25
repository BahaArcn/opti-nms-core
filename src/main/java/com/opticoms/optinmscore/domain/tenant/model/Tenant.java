package com.opticoms.optinmscore.domain.tenant.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
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

    private boolean active = true;
}
