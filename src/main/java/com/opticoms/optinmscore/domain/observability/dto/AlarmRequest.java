package com.opticoms.optinmscore.domain.observability.dto;

import com.opticoms.optinmscore.domain.observability.model.Alarm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlarmRequest {
    @NotBlank
    private String source;

    @NotBlank
    private String alarmType;

    private String description;

    @NotNull
    private Alarm.Severity severity;
}
