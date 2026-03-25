package com.opticoms.optinmscore.domain.system.update.dto;

import lombok.Data;

@Data
public class UpdateCheckResult {
    private String currentBuildTime;
    private String hubLastUpdated;
    private boolean updateAvailable;
    private String dockerImage;
    private String checkedAt;
    private String message;
}
