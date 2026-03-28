package com.opticoms.optinmscore.domain.apn.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import com.opticoms.optinmscore.common.model.IpFilterRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "apn_profiles")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_dnn_sst_idx",
                def = "{'tenantId': 1, 'dnn': 1, 'sst': 1}", unique = true),
        @CompoundIndex(name = "tenant_status_idx", def = "{'tenantId': 1, 'status': 1}")
})
@Schema(description = "APN/DNN profile defining data network access rules and QoS policies")
public class ApnProfile extends BaseEntity {

    @NotBlank
    @Schema(description = "Data Network Name", example = "internet")
    private String dnn;

    @NotNull
    @Min(1) @Max(255)
    @Schema(description = "Slice/Service Type (1=eMBB, 2=uRLLC, 3=MIoT, 4=V2X, 5=HMTC, 128-255=operator-specific)", example = "1")
    private Integer sst;

    @Schema(description = "Slice Differentiator (hex, optional)", example = "000001")
    private String sd;

    @NotNull
    @Schema(description = "PDU session type", example = "IPV4")
    private PduSessionType pduSessionType;

    @NotNull @Valid
    @Schema(description = "Default 5QI / QoS class for this DNN")
    private QosProfile qos;

    @Valid
    @Schema(description = "Session AMBR (Aggregate Maximum Bit Rate)")
    private Ambr sessionAmbr;

    @Schema(description = "Whether this APN/DNN profile is active", example = "true")
    private boolean enabled = true;

    @Schema(description = "Profile status", example = "ACTIVE")
    private ProfileStatus status = ProfileStatus.ACTIVE;

    @Schema(description = "Human-readable description", example = "Default internet APN for eMBB slice")
    private String description;

    @Schema(description = "Named bandwidth policy label", example = "High")
    private String bandwidthPolicyName;

    @Schema(description = "Enable IP filtering for this DNN")
    private boolean ipFilteringEnabled = false;

    @Valid
    @Schema(description = "IP filter rules (evaluated in order)")
    private List<IpFilterRule> ipFilterRules;

    public enum PduSessionType {
        @Schema(description = "IPv4 only") IPV4,
        @Schema(description = "IPv6 only") IPV6,
        @Schema(description = "IPv4 and IPv6 dual-stack") IPV4V6
    }

    public enum ProfileStatus {
        ACTIVE, INACTIVE, DEPRECATED
    }

    @Data
    @Schema(description = "QoS parameters for default bearer")
    public static class QosProfile {

        @NotNull
        @Min(1) @Max(86)
        @Schema(description = "5G QoS Identifier (5QI). 1-4=GBR conv, 5=non-GBR IMS, 9=default internet",
                example = "9")
        private Integer fiveQi;

        @NotNull
        @Min(1) @Max(15)
        @Schema(description = "Allocation and Retention Priority level (1=highest, 15=lowest)",
                example = "8")
        private Integer arpPriorityLevel;

        @Schema(description = "ARP pre-emption capability", example = "NOT_PRE_EMPT")
        private PreEmption preEmptionCapability = PreEmption.NOT_PRE_EMPT;

        @Schema(description = "ARP pre-emption vulnerability", example = "PRE_EMPTABLE")
        private PreEmption preEmptionVulnerability = PreEmption.PRE_EMPTABLE;
    }

    @Data
    @Schema(description = "Aggregate Maximum Bit Rate")
    public static class Ambr {

        @NotNull @Min(0)
        @Schema(description = "Uplink AMBR in kbps", example = "1000000")
        private Long uplinkKbps;

        @NotNull @Min(0)
        @Schema(description = "Downlink AMBR in kbps", example = "2000000")
        private Long downlinkKbps;
    }

    public enum PreEmption {
        PRE_EMPT, NOT_PRE_EMPT, PRE_EMPTABLE, NOT_PRE_EMPTABLE
    }
}
