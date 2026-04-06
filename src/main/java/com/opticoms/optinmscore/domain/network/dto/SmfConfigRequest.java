package com.opticoms.optinmscore.domain.network.dto;

import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class SmfConfigRequest {
    @Min(1280) @Max(1500)
    private Integer smfMtu;

    private List<String> smfDnsIps;
    private SmfConfig.SecurityIndication securityIndication;

    @Min(500) @Max(1460)
    private int tcpMss = 1340;

    @Min(60) @Max(86400)
    private int dhcpLeaseTimeSec = 7200;

    @Pattern(regexp = "^$|^([0-9]{1,3}\\.){3}[0-9]{1,3}$")
    private String proxyCscfIp;

    @NotEmpty(message = "APN/DNN list cannot be empty")
    @Valid
    private List<SmfConfig.ApnDnn> apnList;
}
