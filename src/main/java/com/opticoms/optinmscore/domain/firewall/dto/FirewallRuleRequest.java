package com.opticoms.optinmscore.domain.firewall.dto;

import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FirewallRuleRequest {
    @NotNull
    private FirewallRule.Chain chain;

    @NotNull
    private FirewallRule.Protocol protocol;

    @Pattern(regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)(\\/([0-9]|[1-2][0-9]|3[0-2]))?$",
            message = "must be a valid IPv4 address or CIDR notation")
    private String sourceIp;

    @Pattern(regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)(\\/([0-9]|[1-2][0-9]|3[0-2]))?$",
            message = "must be a valid IPv4 address or CIDR notation")
    private String destinationIp;

    private Integer sourcePort;
    private Integer destinationPort;

    @NotNull
    private FirewallRule.Action action;

    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9._-]{0,14}$",
            message = "must be a valid network interface name")
    private String interfaceName;

    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9._-]{0,14}$",
            message = "must be a valid network interface name")
    private String outInterfaceName;

    @NotBlank
    private String description;

    private int priority;
    private boolean enabled = true;
}
