package com.opticoms.optinmscore.domain.observability.dto;

import com.opticoms.optinmscore.domain.observability.model.Alarm;
import lombok.Data;

@Data
public class AlarmResponse {
    private String id;
    private String source;
    private String alarmType;
    private String description;
    private Alarm.Severity severity;
    private Alarm.AlarmStatus status;
    private Long eventTime;
    private Long clearedTime;
    private String clearedBy;
    private Long acknowledgedTime;
    private String acknowledgedBy;
    private Long createdAt;
    private Long updatedAt;
}
