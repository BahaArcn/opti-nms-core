package com.opticoms.optinmscore.domain.network.dto;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import lombok.Data;

import java.util.List;

@Data
public class GlobalConfigResponse {
    private String id;
    private String networkFullName;
    private String networkShortName;
    private GlobalConfig.NetworkMode networkMode;
    private int maxSupportedDevices;
    private int maxSupportedGNBs;
    private boolean workAsMaster;
    private String masterAddr;
    private List<GlobalConfig.Tai> taiList;
    private int mtu;
    private List<String> dnsIps;
    private List<GlobalConfig.UeIpPool> ueIpPoolList;
    private Integer defaultFiveQi;
    private Integer defaultArpPriority;
    private Long defaultAmbrUlKbps;
    private Long defaultAmbrDlKbps;
    private Integer udmAmf;
    private GlobalConfig.AuthMethod authMethod;
    private boolean encryptClientSignaling;
    private Long createdAt;
    private Long updatedAt;
}
