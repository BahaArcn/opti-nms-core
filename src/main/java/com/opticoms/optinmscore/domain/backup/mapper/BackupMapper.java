package com.opticoms.optinmscore.domain.backup.mapper;

import com.opticoms.optinmscore.domain.backup.dto.BackupResponse;
import com.opticoms.optinmscore.domain.backup.model.BackupEntry;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface BackupMapper {

    BackupResponse toResponse(BackupEntry entity);

    List<BackupResponse> toResponseList(List<BackupEntry> entities);
}
