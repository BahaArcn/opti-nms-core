package com.opticoms.optinmscore.domain.multitenant.mapper;

import com.opticoms.optinmscore.domain.multitenant.dto.SlaveNodeResponse;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface SlaveNodeMapper {
    SlaveNodeResponse toResponse(SlaveNode entity);
}
