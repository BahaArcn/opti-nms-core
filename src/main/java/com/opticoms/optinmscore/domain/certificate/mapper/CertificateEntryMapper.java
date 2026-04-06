package com.opticoms.optinmscore.domain.certificate.mapper;

import com.opticoms.optinmscore.domain.certificate.dto.CertificateEntryRequest;
import com.opticoms.optinmscore.domain.certificate.dto.CertificateEntryResponse;
import com.opticoms.optinmscore.domain.certificate.model.CertificateEntry;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface CertificateEntryMapper {
    CertificateEntry toEntity(CertificateEntryRequest request);
    CertificateEntryResponse toResponse(CertificateEntry entity);
    List<CertificateEntryResponse> toResponseList(List<CertificateEntry> entities);
}
