package com.opticoms.optinmscore.domain.tenant.mapper;

import com.opticoms.optinmscore.domain.tenant.dto.TenantRequest;
import com.opticoms.optinmscore.domain.tenant.dto.TenantResponse;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    Tenant toEntity(TenantRequest request);

    TenantResponse toResponse(Tenant entity);

    List<TenantResponse> toResponseList(List<Tenant> entities);
}
