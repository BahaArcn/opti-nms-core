package com.opticoms.optinmscore.domain.certificate.model;

import com.opticoms.optinmscore.common.model.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "certificates")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_name_idx", def = "{'tenantId': 1, 'name': 1}", unique = true),
        @CompoundIndex(name = "tenant_type_status_idx", def = "{'tenantId': 1, 'certType': 1, 'status': 1}")
})
@Schema(description = "X.509 certificate managed by OptiNMS for NF TLS/mTLS")
public class CertificateEntry extends BaseEntity {

    @NotBlank
    @Schema(description = "Unique certificate name / alias", example = "amf-server-cert")
    private String name;

    @NotNull
    @Schema(description = "Certificate type", example = "SERVER")
    private CertType certType;

    @NotBlank
    @Schema(description = "PEM-encoded X.509 certificate body (-----BEGIN CERTIFICATE-----...)")
    private String certificatePem;

    @Schema(description = "PEM-encoded private key (stored encrypted, null for CA/trust certs)")
    private String privateKeyPem;

    @Schema(description = "PEM-encoded CA chain (intermediate + root, optional)")
    private String caCertificateChainPem;

    @Schema(description = "Subject DN extracted from certificate", example = "CN=amf.open5gs.org",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String subjectDn;

    @Schema(description = "Issuer DN extracted from certificate", example = "CN=OptiNMS Internal CA",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String issuerDn;

    @Schema(description = "Serial number (hex) extracted from certificate",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String serialNumber;

    @Schema(description = "Certificate notBefore (epoch ms)", accessMode = Schema.AccessMode.READ_ONLY)
    private Long notBefore;

    @Schema(description = "Certificate notAfter / expiry (epoch ms)", accessMode = Schema.AccessMode.READ_ONLY)
    private Long notAfter;

    @Schema(description = "Certificate lifecycle status", example = "ACTIVE")
    private CertStatus status = CertStatus.ACTIVE;

    @Schema(description = "Human-readable description", example = "AMF server TLS certificate for N2 interface")
    private String description;

    public enum CertType {
        @Schema(description = "Server certificate (TLS)")
        SERVER,
        @Schema(description = "Client certificate (mTLS)")
        CLIENT,
        @Schema(description = "Certificate Authority (root or intermediate)")
        CA
    }

    public enum CertStatus {
        ACTIVE,
        INACTIVE,
        EXPIRED,
        REVOKED
    }
}
