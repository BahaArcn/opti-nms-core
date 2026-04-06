package com.opticoms.optinmscore.domain.backup.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "backup_entries")
public class BackupEntry extends BaseEntity {

    private String filename;
    private long sizeBytes;
    private BackupStatus status;
    private String storagePath;
    private String triggeredBy;
    private Long completedAt;
    private String errorMessage;

    public enum BackupStatus { IN_PROGRESS, COMPLETED, FAILED }
}
