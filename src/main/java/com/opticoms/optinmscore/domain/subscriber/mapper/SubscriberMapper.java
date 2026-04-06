package com.opticoms.optinmscore.domain.subscriber.mapper;

import com.opticoms.optinmscore.domain.subscriber.dto.SubscriberRequest;
import com.opticoms.optinmscore.domain.subscriber.dto.SubscriberResponse;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface SubscriberMapper {

    Subscriber toEntity(SubscriberRequest request);

    SubscriberResponse toResponse(Subscriber entity);

    List<SubscriberResponse> toResponseList(List<Subscriber> entities);
}
