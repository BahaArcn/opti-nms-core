package com.opticoms.optinmscore.domain.inventory.dto;

import com.opticoms.optinmscore.domain.inventory.model.PduSession;
import lombok.Data;

@Data
public class PduSessionResponse {
    private String id;
    private String sessionId;
    private String imsi;
    private String dnn;
    private String ueIpAddress;
    private int sst;
    private String sd;
    private PduSession.SessionStatus status;
    private Long establishedAt;
    private Long lastSeenAt;
    private Long createdAt;
    private Long updatedAt;
}
