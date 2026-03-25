package com.opticoms.optinmscore.domain.system.update.dto;

import lombok.Data;

@Data
public class VersionInfo {
    private String appVersion;
    private String buildTime;
    private String dockerImage;
    private String deployMode;
}
