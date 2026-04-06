package com.opticoms.optinmscore.domain.apn.dto;

import com.opticoms.optinmscore.common.model.IpFilterRule;
import com.opticoms.optinmscore.domain.apn.model.ApnProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ApnProfileRequest {
    @NotBlank
    private String dnn;

    @NotNull @Min(1) @Max(255)
    private Integer sst;

    private String sd;

    @NotNull
    private ApnProfile.PduSessionType pduSessionType;

    @NotNull @Valid
    private ApnProfile.QosProfile qos;

    @Valid
    private ApnProfile.Ambr sessionAmbr;

    private boolean enabled = true;
    private String description;
    private String bandwidthPolicyName;
    private boolean ipFilteringEnabled;

    @Valid
    private List<IpFilterRule> ipFilterRules;
}
