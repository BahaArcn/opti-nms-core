package com.opticoms.optinmscore.domain.license.mapper;

import com.opticoms.optinmscore.domain.license.dto.LicenseRequest;
import com.opticoms.optinmscore.domain.license.dto.LicenseResponse;
import com.opticoms.optinmscore.domain.license.model.License;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface LicenseMapper {

    License toEntity(LicenseRequest request);

    LicenseResponse toResponse(License entity);
}
