package com.opticoms.optinmscore.domain.network.dto;

import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AmfConfigRequest {
    private String amfName;
    private String mmeName;

    @Valid
    private AmfConfig.AmfId amfId;

    @Valid
    private AmfConfig.MmeId mmeId;

    private String n2InterfaceIp;
    private String s1cInterfaceIp;

    @NotEmpty
    @Valid
    private List<AmfConfig.Plmn> supportedPlmns;

    @NotEmpty
    @Valid
    private List<AmfConfig.Tai> supportedTais;

    @Valid
    private List<AmfConfig.Slice> supportedSlices;

    private AmfConfig.SecurityParameters securityParameters;
    private AmfConfig.NasTimers5g nasTimers5g;
    private AmfConfig.NasTimers4g nasTimers4g;
}
