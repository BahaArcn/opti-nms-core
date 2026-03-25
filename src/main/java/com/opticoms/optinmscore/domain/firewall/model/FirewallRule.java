package com.opticoms.optinmscore.domain.firewall.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "firewall_rules")
@CompoundIndex(name = "tenant_priority_idx", def = "{'tenantId': 1, 'priority': 1}")
@Schema(description = "iptables-based firewall rule managed at OS level")
public class FirewallRule extends BaseEntity {

    @NotNull
    @Schema(description = "iptables chain", example = "INPUT")
    private Chain chain;

    @NotNull
    @Schema(description = "Network protocol", example = "TCP")
    private Protocol protocol;

    @Pattern(
        regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)"
               + "(\\/([0-9]|[1-2][0-9]|3[0-2]))?$",
        message = "must be a valid IPv4 address or CIDR notation (e.g. 10.45.0.0/16)"
    )
    @Schema(description = "Source IP/CIDR (null = any)", example = "10.45.0.0/16")
    private String sourceIp;

    @Pattern(
        regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)"
               + "(\\/([0-9]|[1-2][0-9]|3[0-2]))?$",
        message = "must be a valid IPv4 address or CIDR notation (e.g. 192.168.0.0/24)"
    )
    @Schema(description = "Destination IP/CIDR (null = any)", example = "192.168.0.0/24")
    private String destinationIp;

    @Schema(description = "Source port (null = any)", example = "0")
    private Integer sourcePort;

    @Schema(description = "Destination port (null = any)", example = "8080")
    private Integer destinationPort;

    @NotNull
    @Schema(description = "Rule action", example = "ACCEPT")
    private Action action;

    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z0-9._-]{0,14}$",
        message = "must be a valid network interface name (1-15 chars, alphanumeric/._-)"
    )
    @Schema(description = "Inbound network interface (null = any)", example = "ogstun")
    private String interfaceName;

    @Pattern(
        regexp = "^[a-zA-Z][a-zA-Z0-9._-]{0,14}$",
        message = "must be a valid network interface name (1-15 chars, alphanumeric/._-)"
    )
    @Schema(description = "Outbound network interface for FORWARD chain (null = any)", example = "eth0")
    private String outInterfaceName;

    @NotBlank
    @Schema(description = "Human-readable rule description", example = "Allow GTP-U traffic on N3 interface")
    private String description;

    @Schema(description = "Rule priority / ordering (lower = higher priority)", example = "100")
    private int priority;

    @Schema(description = "Whether this rule is enabled", example = "true")
    private boolean enabled = true;

    @Schema(description = "Current rule status on the OS", example = "PENDING", accessMode = Schema.AccessMode.READ_ONLY)
    private RuleStatus ruleStatus = RuleStatus.PENDING;

    @Schema(description = "Last error message if apply failed", accessMode = Schema.AccessMode.READ_ONLY)
    private String lastError;

    public enum Chain {
        INPUT, OUTPUT, FORWARD
    }

    public enum Protocol {
        TCP, UDP, ICMP, ALL
    }

    public enum Action {
        ACCEPT, DROP, REJECT, LOG
    }

    public enum RuleStatus {
        PENDING, APPLIED, FAILED, REMOVED
    }
}
