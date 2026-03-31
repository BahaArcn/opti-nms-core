package com.opticoms.optinmscore.domain.network.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "amf_configs")
public class AmfConfig extends BaseEntity {

    @Schema(description = "AMF name (required when 5G or HYBRID mode)")
    private String amfName;

    @Schema(description = "MME name (required when 4G or HYBRID mode)")
    private String mmeName;

    @Valid
    @Schema(description = "AMF identifier: region/set/pointer (required when 5G or HYBRID mode)")
    private AmfId amfId;

    @Valid
    @Schema(description = "MME identifier: MMEGI/MMEC (required when 4G or HYBRID mode)")
    private MmeId mmeId;

    @Schema(description = "NGAP interface IP (5G)")
    private String n2InterfaceIp;

    @Schema(description = "S1AP interface IP (4G)")
    private String s1cInterfaceIp;

    @NotEmpty
    private List<@Valid Plmn> supportedPlmns;

    @NotEmpty
    private List<@Valid Tai> supportedTais;

    @Schema(description = "Network slices (required when 5G or HYBRID mode)")
    private List<@Valid Slice> supportedSlices;

    private SecurityParameters securityParameters = new SecurityParameters();

    @Schema(description = "5G NAS timers (required when 5G or HYBRID mode)")
    private NasTimers5g nasTimers5g = new NasTimers5g();

    @Schema(description = "4G NAS timers (required when 4G or HYBRID mode)")
    private NasTimers4g nasTimers4g = new NasTimers4g();

    // =========================================
    // INNER CLASSES (Alt Modeller)
    // =========================================

    @Data
    public static class AmfId {
        @Min(0) @Max(255) // 8 bits
        private int region;

        @Min(0) @Max(1023) // 10 bits
        private int set;

        @Min(0) @Max(63) // 6 bits
        private int pointer;
    }

    @Data
    public static class MmeId {
        @Min(0) @Max(65535) // 16 bits
        private int mmegi;

        @Min(0) @Max(255) // 8 bits
        private int mmec;
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

        @Min(0) @Max(65533)
        @Schema(description = "TAC range end (if > tac, defines a range; otherwise single TAC)")
        private int tacEnd;
    }

    @Data
    public static class Slice {
        @Min(1) @Max(255)
        private int sst;

        private String sd = "FFFFFF"; // Default
    }

    @Data
    public static class SecurityParameters {
        // ✅ DÜZELTİLDİ: List.of() yerine new ArrayList<>(List.of())
        // Artık liste mutable (değiştirilebilir)

        private List<String> integrityOrder5g = new ArrayList<>(
                List.of("NIA2", "NIA1", "NIA0")
        );

        private List<String> cipheringOrder5g = new ArrayList<>(
                List.of("NEA0", "NEA1", "NEA2")
        );

        private List<String> integrityOrder4g = new ArrayList<>(
                List.of("EIA2", "EIA1", "EIA0")
        );

        private List<String> cipheringOrder4g = new ArrayList<>(
                List.of("EEA0", "EEA1", "EEA2")
        );
    }


    @Data
    public static class NasTimers5g {
        private int t3502 = 720;
        private int t3512 = 540;
    }

    @Data
    public static class NasTimers4g {
        private int t3402 = 720;
        private int t3412 = 3240;
        private int t3423 = 720;
    }
}