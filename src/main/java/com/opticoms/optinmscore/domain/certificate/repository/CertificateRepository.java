package com.opticoms.optinmscore.domain.certificate.repository;

import com.opticoms.optinmscore.domain.certificate.model.CertificateEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends MongoRepository<CertificateEntry, String> {

    Page<CertificateEntry> findByTenantId(String tenantId, Pageable pageable);

    Optional<CertificateEntry> findByIdAndTenantId(String id, String tenantId);

    Optional<CertificateEntry> findByTenantIdAndName(String tenantId, String name);

    List<CertificateEntry> findByTenantIdAndCertType(String tenantId, CertificateEntry.CertType certType);

    List<CertificateEntry> findByTenantIdAndStatus(String tenantId, CertificateEntry.CertStatus status);

    List<CertificateEntry> findByTenantIdAndNotAfterLessThanAndStatusNot(
            String tenantId, Long epochMs, CertificateEntry.CertStatus excludeStatus);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, CertificateEntry.CertStatus status);
}
