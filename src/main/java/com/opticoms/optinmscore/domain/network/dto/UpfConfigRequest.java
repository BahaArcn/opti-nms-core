package com.opticoms.optinmscore.domain.network.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpfConfigRequest {
    @Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$",
            message = "N3 Interface IP must be a valid IPv4 address")
    private String n3InterfaceIp;

    @Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$",
            message = "S1-U Interface IP must be a valid IPv4 address")
    private String s1uInterfaceIp;

    @Pattern(regexp = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$",
            message = "N4 (PFCP) Interface IP must be a valid IPv4 address")
    private String n4PfcpIp;
}
