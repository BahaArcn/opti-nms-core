package com.opticoms.optinmscore.domain.network.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "smf_configs")
public class SmfConfig extends BaseEntity {

    // Tabloda 1280 ile 1500 arası istenmiş, default 1400
    @Min(1280)
    @Max(1500)
    private int mtu = 1400;

    // DNS IP'leri: En az 1 tane girilmesi zorunlu
    @NotEmpty(message = "At least one DNS IP is required")
    private List<String> dnsIps;

    // 5G Güvenlik Belirteçleri
    @NotNull
    private SecurityIndication securityIndication = new SecurityIndication();

    // APN/DNN Listesi
    @NotEmpty(message = "APN/DNN list cannot be empty")
    private List<@Valid ApnDnn> apnList;

    // =========================================
    // INNER CLASSES & ENUMS (Alt Modeller)
    // =========================================

    // Tablodaki "required, preferred or not-needed" kuralı
    public enum RequirementLevel {
        REQUIRED, PREFERRED, NOT_NEEDED
    }

    @Data
    public static class SecurityIndication {
        @NotNull
        private RequirementLevel integrity = RequirementLevel.NOT_NEEDED;

        @NotNull
        private RequirementLevel ciphering = RequirementLevel.NOT_NEEDED;
    }

    @Data
    public static class ApnDnn {

        @NotBlank
        // SADECE CIDR (x.x.x.x/y) veya IP Aralığı (x.x.x.x-y.y.y.y) kabul eden özel Regex
        @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}(/[0-9]{1,2}|-([0-9]{1,3}\\.){3}[0-9]{1,3})$",
                message = "Must be in CIDR format (e.g., 10.45.0.0/16) or Range format (e.g., 10.45.0.100-10.45.0.200)")
        private String ueIpRange;

        @NotBlank
        private String gatewayIp;

        @NotBlank
        private String apnDnnName;

        @Schema(description = "Whether DNN is served by local UPF (true) or remote UPF (false)", example = "true")
        private boolean local = true;

        @Pattern(regexp = "^$|^([0-9]{1,3}\\.){3}[0-9]{1,3}$", message = "Must be a valid IPv4 address")
        @Schema(description = "Remote UPF IP address (only required when local=false)")
        private String remoteUpfIp;

        private SliceId sliceId;
        private Tai tai;
    }

    @Data
    public static class SliceId {
        @Min(1) @Max(255)
        private int sst;
        private String sd = "FFFFFF";
    }

    @Data
    public static class Tai {
        @Valid @NotNull
        private Plmn plmn;

        @Min(1) @Max(65533)
        private int tac;
    }

    @Data
    public static class Plmn {
        @Pattern(regexp = "\\d{3}")
        private String mcc;

        @Pattern(regexp = "\\d{2,3}")
        private String mnc;
    }
}