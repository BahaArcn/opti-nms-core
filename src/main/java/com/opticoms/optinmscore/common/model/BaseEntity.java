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
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    /**
     * Tenant ID for multi-tenant support.
     * Taken from the request header in controllers; not expected in the body.
     */
    @Indexed
    @Pattern(regexp = "^[A-Z]{4}-\\d{4}/\\d{4}/\\d{2}$",
            message = "Tenant-ID must follow format: XXXX-DDDD/DDDD/DD (e.g. VFTR-0001/0001/01)")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY,
            description = "Tenant ID (format: XXXX-DDDD/DDDD/DD)",
            example = "VFTR-0001/0001/01")
    private String tenantId;

    // ============================================
    // AUDIT FIELDS (auto-populated, read-only)
    // ============================================

    @CreatedDate
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long createdAt;

    @LastModifiedDate
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long updatedAt;

    @CreatedBy
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;

    @LastModifiedBy
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String lastModifiedBy;

    /**
     * Version field for optimistic locking (managed by Spring).
     */
    @Version
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long version;
}