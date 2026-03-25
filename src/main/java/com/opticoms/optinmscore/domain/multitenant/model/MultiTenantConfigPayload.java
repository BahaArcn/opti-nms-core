package com.opticoms.optinmscore.domain.multitenant.model;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import lombok.Data;

@Data
public class MultiTenantConfigPayload {
    private String networkFullName;
    private String networkShortName;
    private GlobalConfig.NetworkMode networkMode;
}
