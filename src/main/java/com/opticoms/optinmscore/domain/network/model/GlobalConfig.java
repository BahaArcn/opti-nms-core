package com.opticoms.optinmscore.domain.network.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "global_configs")
public class GlobalConfig extends BaseEntity {

    @NotBlank(message = "Network Full Name is required")
    private String networkFullName;

    @NotBlank(message = "Network Short Name is required")
    private String networkShortName;

    @NotNull(message = "Network Mode is required")
    private NetworkMode networkMode;

    // Tablo default değeri: 1024
    @Min(1)
    @Max(100000)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private int maxSupportedDevices = 1024;

    // Tablo default değeri: 64
    @Min(1)
    @Max(1000)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private int maxSupportedGNBs = 64;

    @Schema(description = "Whether this instance operates as master in multi-tenant architecture", example = "false")
    private boolean workAsMaster = false;

    @Schema(description = "Master instance address (IPv4/IPv6/domain) when workAsMaster=false")
    private String masterAddr;

    public enum NetworkMode {
        ONLY_5G,
        ONLY_4G,
        HYBRID_4G_5G
    }
}