package com.opticoms.optinmscore.domain.inventory.mapper;

import com.opticoms.optinmscore.domain.inventory.dto.*;
import com.opticoms.optinmscore.domain.inventory.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface InventoryMapper {
    GNodeBResponse toGNodeBResponse(GNodeB entity);
    ConnectedUeResponse toConnectedUeResponse(ConnectedUe entity);
    PduSessionResponse toPduSessionResponse(PduSession entity);
    NodeResourceResponse toNodeResourceResponse(NodeResource entity);
    NodeResource toNodeResourceEntity(NodeResourceRequest request);
}
