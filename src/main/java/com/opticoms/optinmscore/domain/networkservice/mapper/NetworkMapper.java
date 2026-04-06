package com.opticoms.optinmscore.domain.networkservice.mapper;

import com.opticoms.optinmscore.domain.networkservice.dto.NetworkRequest;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkResponse;
import com.opticoms.optinmscore.domain.networkservice.model.Network;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface NetworkMapper {
    Network toEntity(NetworkRequest request);
    NetworkResponse toResponse(Network entity);
}
