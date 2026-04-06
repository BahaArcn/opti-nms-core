package com.opticoms.optinmscore.domain.network.dto;

import lombok.Data;

@Data
public class UpfConfigResponse {
    private String id;
    private String n3InterfaceIp;
    private String s1uInterfaceIp;
    private String n4PfcpIp;
    private Long createdAt;
    private Long updatedAt;
}
