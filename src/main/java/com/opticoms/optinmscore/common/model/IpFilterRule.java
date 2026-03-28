package com.opticoms.optinmscore.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "IP filtering rule for traffic control")
public class IpFilterRule {

    @NotNull
    private IpFilterAction action;

    @NotBlank
    @Schema(description = "CIDR notation", example = "10.0.0.0/8")
    private String cidr;

    @Schema(description = "Port number (null = any port)")
    private Integer port;

    @Schema(description = "Protocol: TCP, UDP, or ANY", example = "ANY")
    private String protocol;

    public enum IpFilterAction {
        ALLOW, DENY
    }
}
