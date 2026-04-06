package com.opticoms.optinmscore.domain.policy.dto;

import com.opticoms.optinmscore.common.model.IpFilterRule;
import com.opticoms.optinmscore.domain.policy.model.Policy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PolicyRequest {
    @NotBlank
    private String name;
    private String description;
    private boolean enabled = true;

    @Valid
    private Policy.BandwidthLimit bandwidthLimit;

    private Policy.RatType ratPreference;

    @Min(1) @Max(256)
    private Integer frequencySelectionPriority;

    private boolean ipFilteringEnabled;

    @Valid
    private List<IpFilterRule> ipFilterRules;

    private List<Integer> allowedTacs;

    @Valid
    private Policy.TimeSchedule timeSchedule;

    @Valid
    private List<Policy.SliceConfig> defaultSlices;
}
