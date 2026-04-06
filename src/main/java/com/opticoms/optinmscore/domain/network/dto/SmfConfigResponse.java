package com.opticoms.optinmscore.domain.network.dto;

import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import lombok.Data;

import java.util.List;

@Data
public class SmfConfigResponse {
    private String id;
    private Integer smfMtu;
    private List<String> smfDnsIps;
    private SmfConfig.SecurityIndication securityIndication;
    private int tcpMss;
    private int dhcpLeaseTimeSec;
    private String proxyCscfIp;
    private List<SmfConfig.ApnDnn> apnList;
    private Long createdAt;
    private Long updatedAt;
}
