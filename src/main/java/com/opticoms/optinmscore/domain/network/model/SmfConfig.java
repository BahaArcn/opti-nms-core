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

    @Min(1280)
    @Max(1500)
    @Schema(description = "SMF-specific MTU override. If not set, falls back to GlobalConfig.mtu", example = "1400")
    private Integer smfMtu;

    @Schema(description = "SMF-specific DNS override. If not set, falls back to GlobalConfig.dnsIps")
    private List<String> smfDnsIps;

    @Schema(description = "5G NAS security indication (required when 5G or HYBRID mode)")
    private SecurityIndication securityIndication = new SecurityIndication();

    @Min(500) @Max(1460)
    @Schema(description = "TCP Maximum Segment Size", example = "1340")
    private int tcpMss = 1340;

    @Min(60) @Max(86400)
    @Schema(description = "UPF DHCP lease time in seconds", example = "7200")
    private int dhcpLeaseTimeSec = 7200;

    @Pattern(regexp = "^$|^([0-9]{1,3}\\.){3}[0-9]{1,3}$")
    @Schema(description = "IMS Proxy-CSCF IP address for VoLTE", example = "10.0.0.100")
    private String proxyCscfIp;

    // APN/DNN list
    @NotEmpty(message = "APN/DNN list cannot be empty")
    private List<@Valid ApnDnn> apnList;

    // =========================================
    // INNER CLASSES & ENUMS
    // =========================================

    /** LLD: required / preferred / not-needed */
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
        @Schema(description = "References a tunInterface in GlobalConfig.ueIpPoolList", example = "ogstun")
        private String tunInterface;

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