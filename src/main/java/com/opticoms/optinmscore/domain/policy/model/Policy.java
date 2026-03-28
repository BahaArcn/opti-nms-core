package com.opticoms.optinmscore.domain.policy.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import com.opticoms.optinmscore.common.model.IpFilterRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "policies")
@CompoundIndex(name = "tenant_name_idx", def = "{'tenantId': 1, 'name': 1}", unique = true)
public class Policy extends BaseEntity {

    @NotBlank
    @Schema(description = "Unique policy name within tenant", example = "Gold Plan")
    private String name;

    @Schema(description = "Human-readable description", example = "Premium policy for enterprise subscribers")
    private String description;

    @Schema(description = "Whether this policy is active")
    private boolean enabled = true;

    // --- #10: Bandwidth Limit ---
    @Valid
    private BandwidthLimit bandwidthLimit;

    // --- #11: RAT/Frequency Selection ---
    @Schema(description = "Preferred RAT type")
    private RatType ratPreference;

    @Min(1) @Max(256)
    @Schema(description = "Frequency selection priority (1=highest)")
    private Integer frequencySelectionPriority;

    // --- #12: IP Filtering ---
    @Schema(description = "Enable IP filtering for this policy")
    private boolean ipFilteringEnabled = false;

    @Valid
    @Schema(description = "IP filter rules (evaluated in order)")
    private List<IpFilterRule> ipFilterRules;

    // --- #13: Location Based ---
    @Schema(description = "Restrict access to specific tracking areas (empty = no restriction)")
    private List<Integer> allowedTacs;

    // --- #14: Time Schedule ---
    @Valid
    private TimeSchedule timeSchedule;

    // --- Default Network Slices ---
    @Valid
    @Schema(description = "Default network slices assigned to subscribers under this policy")
    private List<@Valid SliceConfig> defaultSlices;

    // =========================================
    // INNER CLASSES & ENUMS
    // =========================================

    @Data
    public static class BandwidthLimit {
        @Min(0)
        @Schema(description = "Uplink bandwidth limit in kbps")
        private Long uplinkKbps;

        @Min(0)
        @Schema(description = "Downlink bandwidth limit in kbps")
        private Long downlinkKbps;
    }

    public enum RatType {
        NR_5G, LTE_4G, ANY
    }

    @Data
    public static class SliceConfig {
        @Min(1) @Max(255)
        @Schema(description = "Slice/Service Type (1=eMBB, 2=uRLLC, 3=MIoT, 128-255=operator-specific)", example = "1")
        private int sst;

        @Pattern(regexp = "^[0-9a-fA-F]{6}$", message = "SD must be 6 hex chars")
        @Schema(description = "Slice Differentiator", example = "000001")
        private String sd;
    }

    @Data
    public static class TimeSchedule {
        private boolean enabled = false;

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Format: HH:mm")
        @Schema(description = "Schedule start time", example = "08:00")
        private String startTime;

        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Format: HH:mm")
        @Schema(description = "Schedule end time", example = "22:00")
        private String endTime;

        @Schema(description = "Timezone identifier", example = "Europe/Istanbul")
        private String timezone;

        @Schema(description = "Days of week when policy is active")
        private List<DayOfWeek> activeDays;
    }
}
