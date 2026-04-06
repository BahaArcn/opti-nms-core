package com.opticoms.optinmscore.domain.apn.dto;

import com.opticoms.optinmscore.common.model.IpFilterRule;
import com.opticoms.optinmscore.domain.apn.model.ApnProfile;
import lombok.Data;

import java.util.List;

@Data
public class ApnProfileResponse {
    private String id;
    private String dnn;
    private Integer sst;
    private String sd;
    private ApnProfile.PduSessionType pduSessionType;
    private ApnProfile.QosProfile qos;
    private ApnProfile.Ambr sessionAmbr;
    private boolean enabled;
    private ApnProfile.ProfileStatus status;
    private String description;
    private String bandwidthPolicyName;
    private boolean ipFilteringEnabled;
    private List<IpFilterRule> ipFilterRules;
    private Long createdAt;
    private Long updatedAt;
}
