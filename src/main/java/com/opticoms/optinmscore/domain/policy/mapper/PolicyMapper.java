package com.opticoms.optinmscore.domain.policy.mapper;

import com.opticoms.optinmscore.domain.policy.dto.PolicyRequest;
import com.opticoms.optinmscore.domain.policy.dto.PolicyResponse;
import com.opticoms.optinmscore.domain.policy.model.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PolicyMapper {
    Policy toEntity(PolicyRequest request);
    PolicyResponse toResponse(Policy entity);
}
