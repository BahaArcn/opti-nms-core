package com.opticoms.optinmscore.domain.network.dto;

import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import lombok.Data;

import java.util.List;

@Data
public class AmfConfigResponse {
    private String id;
    private String amfName;
    private String mmeName;
    private AmfConfig.AmfId amfId;
    private AmfConfig.MmeId mmeId;
    private String n2InterfaceIp;
    private String s1cInterfaceIp;
    private List<AmfConfig.Plmn> supportedPlmns;
    private List<AmfConfig.Tai> supportedTais;
    private List<AmfConfig.Slice> supportedSlices;
    private AmfConfig.SecurityParameters securityParameters;
    private AmfConfig.NasTimers5g nasTimers5g;
    private AmfConfig.NasTimers4g nasTimers4g;
    private Long createdAt;
    private Long updatedAt;
}
