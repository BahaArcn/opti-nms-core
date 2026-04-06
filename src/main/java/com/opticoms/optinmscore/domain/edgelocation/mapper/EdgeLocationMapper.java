package com.opticoms.optinmscore.domain.edgelocation.mapper;

import com.opticoms.optinmscore.domain.edgelocation.dto.EdgeLocationRequest;
import com.opticoms.optinmscore.domain.edgelocation.dto.EdgeLocationResponse;
import com.opticoms.optinmscore.domain.edgelocation.model.EdgeLocation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface EdgeLocationMapper {
    EdgeLocation toEntity(EdgeLocationRequest request);
    EdgeLocationResponse toResponse(EdgeLocation entity);
}
