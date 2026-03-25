package com.opticoms.optinmscore.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;

/**
 * Base entity for all MongoDB documents.
 * Provides common fields for all entities.
 */
@Data
public abstract class BaseEntity {

    /**
     * MongoDB document ID (auto-generated)
     */
    @Id
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) // <-- İstek örneğinde GİZLENİR
    private String id;

    /**
     * Tenant ID for multi-tenant support.
     * Controller'da Header'dan alındığı için Body'de istenmez.
     */
    @Indexed
    @Pattern(regexp = "^[A-Z]{4}-\\d{4}/\\d{4}/\\d{2}$",
            message = "Tenant-ID must follow format: XXXX-DDDD/DDDD/DD (e.g. VFTR-0001/0001/01)")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY,
            description = "Tenant ID (format: XXXX-DDDD/DDDD/DD)",
            example = "VFTR-0001/0001/01")
    private String tenantId;

    // ============================================
    // AUDIT FIELDS (Otomatik Doldurulur -> Read Only)
    // ============================================

    @CreatedDate
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) // <-- GİZLE
    private Long createdAt;

    @LastModifiedDate
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) // <-- GİZLE
    private Long updatedAt;

    @CreatedBy
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) // <-- GİZLE
    private String createdBy;

    @LastModifiedBy
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) // <-- GİZLE
    private String lastModifiedBy;

    /**
     * Version field for optimistic locking.
     * Kullanıcı bunu elle set etmez, Spring yönetir.
     */
    @Version
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) // <-- GİZLE
    private Long version;
}