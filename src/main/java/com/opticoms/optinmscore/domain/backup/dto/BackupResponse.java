package com.opticoms.optinmscore.domain.backup.dto;

import com.opticoms.optinmscore.domain.backup.model.BackupEntry.BackupStatus;
import lombok.Data;

@Data
public class BackupResponse {
    private String id;
    private String filename;
    private long sizeBytes;
    private BackupStatus status;
    private String triggeredBy;
    private Long createdAt;
    private Long completedAt;
    private String errorMessage;
}
