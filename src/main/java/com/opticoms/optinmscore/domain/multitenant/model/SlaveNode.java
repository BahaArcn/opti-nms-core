package com.opticoms.optinmscore.domain.multitenant.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "slave_nodes")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_slaveAddr_idx", def = "{'tenantId': 1, 'slaveAddress': 1}", unique = true)
})
public class SlaveNode extends BaseEntity {

    @NotBlank
    private String slaveAddress;

    @NotBlank
    private String slaveTenantId;

    private SlaveStatus status;

    private Date lastHeartbeat;

    private Date registeredAt;

    public enum SlaveStatus {
        ONLINE, OFFLINE
    }
}
