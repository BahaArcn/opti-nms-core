package com.opticoms.optinmscore.domain.networkservice.mapper;

import com.opticoms.optinmscore.domain.networkservice.dto.ServiceInstanceRequest;
import com.opticoms.optinmscore.domain.networkservice.dto.ServiceInstanceResponse;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceInstance;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ServiceInstanceMapper {
    ServiceInstance toEntity(ServiceInstanceRequest request);
    ServiceInstanceResponse toResponse(ServiceInstance entity);
}
