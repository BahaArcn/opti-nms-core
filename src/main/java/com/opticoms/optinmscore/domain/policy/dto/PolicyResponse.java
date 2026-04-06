package com.opticoms.optinmscore.domain.policy.dto;

import com.opticoms.optinmscore.common.model.IpFilterRule;
import com.opticoms.optinmscore.domain.policy.model.Policy;
import lombok.Data;

import java.util.List;

@Data
public class PolicyResponse {
    private String id;
    private String name;
    private String description;
    private boolean enabled;
    private Policy.BandwidthLimit bandwidthLimit;
    private Policy.RatType ratPreference;
    private Integer frequencySelectionPriority;
    private boolean ipFilteringEnabled;
    private List<IpFilterRule> ipFilterRules;
    private List<Integer> allowedTacs;
    private Policy.TimeSchedule timeSchedule;
    private List<Policy.SliceConfig> defaultSlices;
    private Long createdAt;
    private Long updatedAt;
}
