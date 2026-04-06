package com.opticoms.optinmscore.domain.suci.dto;

import com.opticoms.optinmscore.domain.suci.model.SuciProfile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SuciProfileRequest {
    @NotNull
    private SuciProfile.ProtectionScheme protectionScheme;

    @NotNull @Min(0) @Max(255)
    private Integer homeNetworkPublicKeyId;

    @NotBlank
    private String homeNetworkPublicKey;

    @NotBlank
    private String homeNetworkPrivateKey;

    private String description;
}
