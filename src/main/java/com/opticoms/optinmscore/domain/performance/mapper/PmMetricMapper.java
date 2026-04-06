package com.opticoms.optinmscore.domain.performance.mapper;

import com.opticoms.optinmscore.domain.performance.dto.PmMetricRequest;
import com.opticoms.optinmscore.domain.performance.dto.PmMetricResponse;
import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PmMetricMapper {
    PmMetric toEntity(PmMetricRequest request);
    PmMetricResponse toResponse(PmMetric entity);
    List<PmMetricResponse> toResponseList(List<PmMetric> entities);
}
