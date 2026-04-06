package com.opticoms.optinmscore.domain.network.mapper;

import com.opticoms.optinmscore.domain.network.dto.*;
import com.opticoms.optinmscore.domain.network.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface NetworkConfigMapper {
    GlobalConfig toGlobalConfigEntity(GlobalConfigRequest request);
    GlobalConfigResponse toGlobalConfigResponse(GlobalConfig entity);

    AmfConfig toAmfConfigEntity(AmfConfigRequest request);
    AmfConfigResponse toAmfConfigResponse(AmfConfig entity);

    SmfConfig toSmfConfigEntity(SmfConfigRequest request);
    SmfConfigResponse toSmfConfigResponse(SmfConfig entity);

    UpfConfig toUpfConfigEntity(UpfConfigRequest request);
    UpfConfigResponse toUpfConfigResponse(UpfConfig entity);
}
