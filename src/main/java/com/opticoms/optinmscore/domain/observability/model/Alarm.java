package com.opticoms.optinmscore.domain.observability.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "alarms")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_status_time_idx", def = "{'tenantId': 1, 'status': 1, 'eventTime': -1}"),
        @CompoundIndex(name = "tenant_sev_status_idx", def = "{'tenantId': 1, 'severity': 1, 'status': 1}"),
        @CompoundIndex(name = "tenant_src_type_status_idx", def = "{'tenantId': 1, 'source': 1, 'alarmType': 1, 'status': 1}")
})
@Schema(description = "Network alarm representing a fault or warning condition")
public class Alarm extends BaseEntity {

    @NotBlank
    @Indexed
    @Schema(description = "Alarm source (e.g. gNodeB ID, network element)", example = "gNodeB-001")
    private String source;

    @NotBlank
    @Indexed
    @Schema(description = "Alarm type identifier", example = "LINK_DOWN")
    private String alarmType;

    @Schema(description = "Human-readable alarm description", example = "S1 interface connection lost")
    private String description;

    @NotNull
    @Indexed
    @Schema(description = "Alarm severity level", example = "CRITICAL")
    private Severity severity;

    @Indexed
    @Schema(description = "Current alarm status", example = "ACTIVE")
    private AlarmStatus status;

    @Indexed
    @Schema(description = "Alarm event time (epoch milliseconds)", example = "1771322400000", accessMode = Schema.AccessMode.READ_ONLY)
    private Long eventTime = System.currentTimeMillis();

    @Schema(description = "Time when alarm was cleared (epoch milliseconds)", accessMode = Schema.AccessMode.READ_ONLY)
    private Long clearedTime;

    @Schema(description = "User who cleared the alarm", accessMode = Schema.AccessMode.READ_ONLY)
    private String clearedBy;

    @Schema(description = "Time when alarm was acknowledged (epoch milliseconds)", accessMode = Schema.AccessMode.READ_ONLY)
    private Long acknowledgedTime;

    @Schema(description = "User who acknowledged the alarm", accessMode = Schema.AccessMode.READ_ONLY)
    private String acknowledgedBy;

    public enum Severity {
        CRITICAL, MAJOR, MINOR, WARNING
    }

    public enum AlarmStatus {
        ACTIVE, ACKNOWLEDGED, CLEARED
    }
}