package com.opticoms.optinmscore.domain.suci.dto;

import com.opticoms.optinmscore.domain.suci.model.SuciProfile;
import lombok.Data;

@Data
public class SuciProfileResponse {
    private String id;
    private SuciProfile.ProtectionScheme protectionScheme;
    private Integer homeNetworkPublicKeyId;
    private String homeNetworkPublicKey;
    private SuciProfile.KeyStatus keyStatus;
    private String description;
    private Long createdAt;
    private Long updatedAt;
}
