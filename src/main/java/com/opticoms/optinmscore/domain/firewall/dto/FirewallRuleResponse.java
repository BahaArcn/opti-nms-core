package com.opticoms.optinmscore.domain.firewall.dto;

import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import lombok.Data;

@Data
public class FirewallRuleResponse {
    private String id;
    private FirewallRule.Chain chain;
    private FirewallRule.Protocol protocol;
    private String sourceIp;
    private String destinationIp;
    private Integer sourcePort;
    private Integer destinationPort;
    private FirewallRule.Action action;
    private String interfaceName;
    private String outInterfaceName;
    private String description;
    private int priority;
    private boolean enabled;
    private FirewallRule.RuleStatus ruleStatus;
    private String lastError;
    private Long createdAt;
    private Long updatedAt;
}
