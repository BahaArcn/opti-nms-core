package com.opticoms.optinmscore.domain.network.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "global_configs")
public class GlobalConfig extends BaseEntity {

    @NotBlank(message = "Network Full Name is required")
    private String networkFullName;

    @NotBlank(message = "Network Short Name is required")
    private String networkShortName;

    @NotNull(message = "Network Mode is required")
    private NetworkMode networkMode;

    /** LLD table default: 1024 */
    @Min(1)
    @Max(100000)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private int maxSupportedDevices = 1024;

    /** LLD table default: 64 */
    @Min(1)
    @Max(1000)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private int maxSupportedGNBs = 64;

    @Schema(description = "Whether this instance operates as master in multi-tenant architecture", example = "false")
    private boolean workAsMaster = false;

    @Schema(description = "Master instance address (IPv4/IPv6/domain) when workAsMaster=false")
    private String masterAddr;

    // --- Network-wide TAI (PLMN + TAC) ---
    @Valid
    @Schema(description = "Network-wide TAI list. Each entry defines a PLMN and TAC served by this network.")
    private List<Tai> taiList;

    // --- MTU ---
    @Min(1280) @Max(1500)
    @Schema(description = "Network-wide MTU value advertised to UEs", example = "1400")
    private int mtu = 1400;

    // --- DNS ---
    @Schema(description = "Network-wide DNS server IPs advertised to UEs", example = "[\"8.8.8.8\", \"8.8.4.4\"]")
    private List<String> dnsIps;

    @Valid
    @Schema(description = "Centralized UE IP pool list. Each DNN references a pool by tunInterface name.")
    private List<UeIpPool> ueIpPoolList;

    // --- Default Traffic QoS ---
    @Min(1) @Max(86)
    @Schema(description = "Default 5QI value for new sessions", example = "9")
    private Integer defaultFiveQi = 9;

    @Min(1) @Max(15)
    @Schema(description = "Default ARP priority level", example = "8")
    private Integer defaultArpPriority = 8;

    @Min(0)
    @Schema(description = "Default UE AMBR uplink in kbps", example = "1000000")
    private Long defaultAmbrUlKbps = 1000000L;

    @Min(0)
    @Schema(description = "Default UE AMBR downlink in kbps", example = "3000000")
    private Long defaultAmbrDlKbps = 3000000L;

    // --- UDM ---
    @Min(0) @Max(65535)
    @Schema(description = "UDM Authentication Management Field (AMF value)", example = "2000")
    private Integer udmAmf = 2000;

    @NotNull
    @Schema(description = "UDM authentication method")
    private AuthMethod authMethod = AuthMethod.FIVE_G_AKA;

    // --- Signaling ---
    @Schema(description = "Global toggle: encrypt NAS signaling messages for all clients")
    private boolean encryptClientSignaling = false;

    @Data
    public static class UeIpPool {
        @NotBlank
        @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}(/[0-9]{1,2}|-([0-9]{1,3}\\.){3}[0-9]{1,3})$",
                message = "Must be CIDR (e.g. 10.45.0.0/16) or range format")
        private String ipRange;

        @NotBlank
        @Schema(description = "TUN interface name, e.g. ogstun, ogstun2", example = "ogstun")
        private String tunInterface;

        @NotBlank
        @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$",
                message = "Must be a valid IPv4 address")
        private String gatewayIp;
    }

    @Data
    public static class Plmn {
        @Pattern(regexp = "\\d{3}", message = "MCC must be 3 digits")
        private String mcc;

        @Pattern(regexp = "\\d{2,3}", message = "MNC must be 2 or 3 digits")
        private String mnc;
    }

    @Data
    public static class Tai {
        @Valid
        @NotNull
        private Plmn plmn;

        @Min(1) @Max(65533)
        private int tac;
    }

    public enum NetworkMode {
        ONLY_5G,
        ONLY_4G,
        HYBRID_4G_5G
    }

    public enum AuthMethod {
        FIVE_G_AKA, EAP_AKA_PRIME
    }
}