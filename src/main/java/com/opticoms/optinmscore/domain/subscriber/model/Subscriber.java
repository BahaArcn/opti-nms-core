package com.opticoms.optinmscore.domain.subscriber.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "subscribers")
@CompoundIndex(name = "tenant_imsi_hash_idx", def = "{'tenantId': 1, 'imsiHash': 1}", unique = true)
public class Subscriber extends BaseEntity {

    @NotBlank
    @Pattern(regexp = "^\\d{15}$", message = "IMSI must be exactly 15 digits")
    @Schema(description = "15-digit IMSI number (stored encrypted)", example = "286010000000001")
    private String imsi;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "SHA-256 hash of IMSI for DB lookups")
    private String imsiHash;

    @Pattern(regexp = "^$|^\\d{10,15}$", message = "MSISDN must be 10-15 digits")
    @Schema(description = "Phone number (stored encrypted)", example = "905551234567")
    private String msisdn;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "SHA-256 hash of MSISDN for DB lookups")
    private String msisdnHash;

    @Size(max = 100, message = "Label must be 100 characters or less")
    @Schema(description = "Human-readable display name for the subscriber (optional, not encrypted)", example = "Baha's Phone")
    private String label;

    // --- Kriptografik Anahtarlar ---

    @NotBlank
    @Schema(description = "Authentication Key (Ki) - 32 hex chars (16 bytes)", example = "465B5CE8B199B49FAA5F0A2EE238A6BC")
    private String ki;

    @NotNull
    @Schema(description = "USIM Type (OP or OPC)", example = "OPC")
    private UsimType usimType;

    @Schema(description = "Operator Code (OPc) - required if usimType=OPC, 32 hex chars", example = "E8ED289DEBA952E4283B54E88E6183CA")
    private String opc;

    @Schema(description = "Operator Code (OP) - required if usimType=OP, 32 hex chars", example = "")
    private String op;

    // --- Hız Limitleri (AMBR) ---

    @Min(0)
    @Schema(description = "UE Downlink Hızı (bps)", example = "1000000000") // 1 Gbps
    private long ueAmbrDl;

    @Min(0)
    @Schema(description = "UE Uplink Hızı (bps)", example = "500000000") // 500 Mbps
    private long ueAmbrUl;

    // --- SIM Type ---
    @Schema(description = "SIM type (optional, user-entered)", example = "SIM_CARD")
    private SimType simType;

    // --- SQN ---
    @Pattern(regexp = "^[0-9a-fA-F]{12}$", message = "SQN must be 12 hex chars (6 bytes)")
    @Schema(description = "Authentication Sequence Number", example = "000000001153")
    private String sqn;

    // --- Roaming ---
    @Schema(description = "Roaming İzni (Local Breakout)", example = "false")
    private boolean lboRoamingAllowed = false;

    // --- Policy Referansı ---
    @Schema(description = "Reference to a Policy document ID (optional)")
    private String policyId;

    // --- Edge Location ---
    @Schema(description = "Reference to an EdgeLocation document ID (optional)")
    private String edgeLocationId;

    // --- Profil Listesi ---
    @NotEmpty(message = "At least one profile is required")
    private List<@Valid SessionProfile> profileList;

    // --- Computed (not persisted) ---
    @Transient
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Computed connection status from ConnectedUe table")
    private ConnectionStatus connectionStatus;


    // =========================================
    // INNER CLASSES & ENUMS
    // =========================================

    public enum UsimType {
        OP, OPC
    }

    public enum SimType {
        SIM_CARD, ESIM, IOT
    }

    public enum ConnectionStatus {
        CONNECTED, DISCONNECTED, UNKNOWN
    }

    @Data
    public static class SessionProfile {

        @Min(1) @Max(255)
        @Schema(description = "Slice Service Type (SST, 1-5=standardized, 128-255=operator-specific)", example = "1")
        private int sst;

        @Schema(description = "Slice Differentiator (SD)", example = "000001")
        private String sd = "FFFFFF";

        @NotBlank
        @Schema(description = "Data Network Name (APN)", example = "internet")
        private String apnDnn;

        @Min(1) @Max(127)
        @Schema(description = "4G QoS Class Identifier", example = "9")
        private int qci4g;

        @Min(1) @Max(127)
        @Schema(description = "5G QoS Identifier", example = "9")
        private int qi5g;

        @Min(1) @Max(3)
        @Schema(description = "PDU Session Type (1=IPv4, 2=IPv6, 3=IPv4v6)", example = "1")
        private int pduType = 1;

        @Min(1) @Max(15)
        @Schema(description = "Allocation Retention Priority (1 en yüksek)", example = "8")
        private int arpPriority;

        @Schema(example = "false")
        private boolean preemptionCapability = false;

        @Schema(example = "false")
        private boolean preemptionVulnerability = false;

        @Schema(description = "Oturum Bazlı DL Hızı", example = "500000000")
        private long sessionAmbrDl;

        @Schema(description = "Oturum Bazlı UL Hızı", example = "250000000")
        private long sessionAmbrUl;

        @Schema(description = "Statik IP (Opsiyonel)", example = "192.168.1.100")
        private String staticIp;
    }
}