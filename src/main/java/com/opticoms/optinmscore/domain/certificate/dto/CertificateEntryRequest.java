package com.opticoms.optinmscore.domain.certificate.dto;

import com.opticoms.optinmscore.domain.certificate.model.CertificateEntry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CertificateEntryRequest {
    @NotBlank
    private String name;

    @NotNull
    private CertificateEntry.CertType certType;

    @NotBlank
    private String certificatePem;

    private String privateKeyPem;
    private String caCertificateChainPem;
    private String description;
}
