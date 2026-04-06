package com.opticoms.optinmscore.domain.certificate.dto;

import com.opticoms.optinmscore.domain.certificate.model.CertificateEntry;
import lombok.Data;

@Data
public class CertificateEntryResponse {
    private String id;
    private String name;
    private CertificateEntry.CertType certType;
    private String certificatePem;
    private String caCertificateChainPem;
    private String subjectDn;
    private String issuerDn;
    private String serialNumber;
    private Long notBefore;
    private Long notAfter;
    private CertificateEntry.CertStatus status;
    private String description;
    private Long createdAt;
    private Long updatedAt;
}
