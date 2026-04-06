package com.opticoms.optinmscore.domain.observability.mapper;

import com.opticoms.optinmscore.domain.observability.dto.AlarmRequest;
import com.opticoms.optinmscore.domain.observability.dto.AlarmResponse;
import com.opticoms.optinmscore.domain.observability.model.Alarm;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface AlarmMapper {
    Alarm toEntity(AlarmRequest request);
    AlarmResponse toResponse(Alarm entity);
}
