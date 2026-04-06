package com.opticoms.optinmscore.domain.network.dto;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class GlobalConfigRequest {
    @NotBlank(message = "Network Full Name is required")
    private String networkFullName;

    @NotBlank(message = "Network Short Name is required")
    private String networkShortName;

    @NotNull(message = "Network Mode is required")
    private GlobalConfig.NetworkMode networkMode;

    private boolean workAsMaster;
    private String masterAddr;

    @Valid
    private List<GlobalConfig.Tai> taiList;

    @Min(1280) @Max(1500)
    private int mtu = 1400;

    private List<String> dnsIps;

    @Valid
    private List<GlobalConfig.UeIpPool> ueIpPoolList;

    @Min(1) @Max(86)
    private Integer defaultFiveQi;

    @Min(1) @Max(15)
    private Integer defaultArpPriority;

    @Min(0)
    private Long defaultAmbrUlKbps;

    @Min(0)
    private Long defaultAmbrDlKbps;

    @Min(0) @Max(65535)
    private Integer udmAmf;

    @NotNull
    private GlobalConfig.AuthMethod authMethod;

    private boolean encryptClientSignaling;
}
