package com.opticoms.optinmscore.domain.suci.mapper;

import com.opticoms.optinmscore.domain.suci.dto.SuciProfileRequest;
import com.opticoms.optinmscore.domain.suci.dto.SuciProfileResponse;
import com.opticoms.optinmscore.domain.suci.model.SuciProfile;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface SuciProfileMapper {
    SuciProfile toEntity(SuciProfileRequest request);
    SuciProfileResponse toResponse(SuciProfile entity);
    List<SuciProfileResponse> toResponseList(List<SuciProfile> entities);
}
