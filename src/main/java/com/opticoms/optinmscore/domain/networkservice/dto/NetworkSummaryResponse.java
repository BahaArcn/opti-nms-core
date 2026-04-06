package com.opticoms.optinmscore.domain.networkservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NetworkSummaryResponse {

    private String networkId;
    private String networkName;
    private long totalServices;
    private long runningServices;
    private long stoppedServices;
    private long errorServices;
}
