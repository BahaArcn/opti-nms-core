package com.opticoms.optinmscore.domain.apn.mapper;

import com.opticoms.optinmscore.domain.apn.dto.ApnProfileRequest;
import com.opticoms.optinmscore.domain.apn.dto.ApnProfileResponse;
import com.opticoms.optinmscore.domain.apn.model.ApnProfile;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ApnProfileMapper {
    ApnProfile toEntity(ApnProfileRequest request);
    ApnProfileResponse toResponse(ApnProfile entity);
    List<ApnProfileResponse> toResponseList(List<ApnProfile> entities);
}
