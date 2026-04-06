package com.opticoms.optinmscore.domain.audit.mapper;

import com.opticoms.optinmscore.domain.audit.dto.AuditLogResponse;
import com.opticoms.optinmscore.domain.audit.model.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface AuditLogMapper {
    AuditLogResponse toResponse(AuditLog entity);
}
