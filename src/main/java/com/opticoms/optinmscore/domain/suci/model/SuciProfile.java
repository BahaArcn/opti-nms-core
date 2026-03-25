package com.opticoms.optinmscore.domain.suci.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "suci_profiles")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_keyId_scheme_idx",
                def = "{'tenantId': 1, 'homeNetworkPublicKeyId': 1, 'protectionScheme': 1}",
                unique = true),
        @CompoundIndex(name = "tenant_status_idx", def = "{'tenantId': 1, 'keyStatus': 1}")
})
@Schema(description = "SUCI (Subscription Concealed Identifier) profile for SUPI protection via ECIES")
public class SuciProfile extends BaseEntity {

    @NotNull
    @Schema(description = "ECIES protection scheme per 3GPP TS 33.501",
            example = "PROFILE_A")
    private ProtectionScheme protectionScheme;

    @NotNull
    @Min(0) @Max(255)
    @Schema(description = "Home Network Public Key Identifier (0-255)", example = "1")
    private Integer homeNetworkPublicKeyId;

    @NotBlank
    @Schema(description = "Home Network Public Key (hex-encoded). " +
            "Profile A: 32-byte X25519 key, Profile B: 33-byte compressed secp256r1 key",
            example = "5a8d38864820197c3394b92613b20b91633cbd897119273bf8e4a6f4eec0a650")
    private String homeNetworkPublicKey;

    @NotBlank
    @Schema(description = "Home Network Private Key (hex-encoded, stored encrypted). " +
            "Used by UDM/SIDF for SUPI de-concealment",
            example = "c080404074080...")
    private String homeNetworkPrivateKey;

    @Schema(description = "Key status", example = "ACTIVE")
    private KeyStatus keyStatus = KeyStatus.ACTIVE;

    @Schema(description = "Human-readable description / label",
            example = "Production HNET key pair for Profile A")
    private String description;

    public enum ProtectionScheme {
        @Schema(description = "No protection (SUPI sent in cleartext)")
        NULL_SCHEME,
        @Schema(description = "ECIES scheme with X25519 + XSalsa20-Poly1305 (3GPP Profile A)")
        PROFILE_A,
        @Schema(description = "ECIES scheme with secp256r1 + AES-128-CTR + HMAC-SHA-256 (3GPP Profile B)")
        PROFILE_B
    }

    public enum KeyStatus {
        ACTIVE,
        INACTIVE,
        REVOKED
    }
}
