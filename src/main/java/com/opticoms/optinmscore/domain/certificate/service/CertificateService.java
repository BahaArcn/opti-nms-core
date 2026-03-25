package com.opticoms.optinmscore.domain.certificate.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.certificate.model.CertificateEntry;
import com.opticoms.optinmscore.domain.certificate.repository.CertificateRepository;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository repository;
    private final EncryptionService encryptionService;

    @Audited(action = AuditAction.CREATE, entityType = "Certificate")
    public CertificateEntry create(String tenantId, CertificateEntry entry) {
        checkDuplicateName(tenantId, entry.getName(), null);
        X509Certificate x509 = parsePem(entry.getCertificatePem());
        populateX509Fields(entry, x509);

        entry.setTenantId(tenantId);
        entry.setStatus(isExpired(x509) ? CertificateEntry.CertStatus.EXPIRED
                : CertificateEntry.CertStatus.ACTIVE);
        encryptPrivateKey(entry);

        log.info("Creating certificate [{}]: type={}, subject={}, tenant={}",
                entry.getName(), entry.getCertType(), entry.getSubjectDn(), tenantId);
        return repository.save(entry);
    }

    public CertificateEntry getById(String tenantId, String id) {
        CertificateEntry entry = findByIdOrThrow(tenantId, id);
        maskPrivateKey(entry);
        return entry;
    }

    public Page<CertificateEntry> list(String tenantId, Pageable pageable) {
        Page<CertificateEntry> page = repository.findByTenantId(tenantId, pageable);
        page.getContent().forEach(this::maskPrivateKey);
        return page;
    }

    public List<CertificateEntry> listByType(String tenantId, CertificateEntry.CertType type) {
        List<CertificateEntry> certs = repository.findByTenantIdAndCertType(tenantId, type);
        certs.forEach(this::maskPrivateKey);
        return certs;
    }

    public List<CertificateEntry> listByStatus(String tenantId, CertificateEntry.CertStatus status) {
        List<CertificateEntry> certs = repository.findByTenantIdAndStatus(tenantId, status);
        certs.forEach(this::maskPrivateKey);
        return certs;
    }

    public List<CertificateEntry> listExpiringSoon(String tenantId, int withinDays) {
        long cutoff = System.currentTimeMillis() + ((long) withinDays * 24 * 60 * 60 * 1000);
        List<CertificateEntry> certs = repository.findByTenantIdAndNotAfterLessThanAndStatusNot(
                tenantId, cutoff, CertificateEntry.CertStatus.REVOKED);
        certs.forEach(this::maskPrivateKey);
        return certs;
    }

    @Audited(action = AuditAction.UPDATE, entityType = "Certificate")
    public CertificateEntry update(String tenantId, String id, CertificateEntry updated) {
        CertificateEntry existing = findByIdOrThrow(tenantId, id);

        if (existing.getStatus() == CertificateEntry.CertStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot update a revoked certificate. Upload a new one instead.");
        }

        checkDuplicateName(tenantId, updated.getName(), id);
        X509Certificate x509 = parsePem(updated.getCertificatePem());
        populateX509Fields(updated, x509);

        existing.setName(updated.getName());
        existing.setCertType(updated.getCertType());
        existing.setCertificatePem(updated.getCertificatePem());
        existing.setPrivateKeyPem(updated.getPrivateKeyPem());
        existing.setCaCertificateChainPem(updated.getCaCertificateChainPem());
        existing.setSubjectDn(updated.getSubjectDn());
        existing.setIssuerDn(updated.getIssuerDn());
        existing.setSerialNumber(updated.getSerialNumber());
        existing.setNotBefore(updated.getNotBefore());
        existing.setNotAfter(updated.getNotAfter());
        existing.setDescription(updated.getDescription());
        if (updated.getStatus() != null) {
            if (updated.getStatus() == CertificateEntry.CertStatus.REVOKED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot set status to REVOKED via update. Use the /revoke endpoint instead.");
            }
            existing.setStatus(updated.getStatus());
        }

        encryptPrivateKey(existing);
        log.info("Updating certificate [{}]: type={}, subject={}", id,
                existing.getCertType(), existing.getSubjectDn());
        return repository.save(existing);
    }

    @Audited(action = AuditAction.DELETE, entityType = "Certificate")
    public void delete(String tenantId, String id) {
        CertificateEntry entry = findByIdOrThrow(tenantId, id);
        log.info("Deleting certificate [{}]: name={}", id, entry.getName());
        repository.delete(entry);
    }

    @Audited(action = AuditAction.REVOKE, entityType = "Certificate")
    public CertificateEntry revoke(String tenantId, String id) {
        CertificateEntry entry = findByIdOrThrow(tenantId, id);

        if (entry.getStatus() == CertificateEntry.CertStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Certificate is already revoked.");
        }

        entry.setStatus(CertificateEntry.CertStatus.REVOKED);
        log.info("Revoking certificate [{}]: name={}, subject={}", id,
                entry.getName(), entry.getSubjectDn());
        return repository.save(entry);
    }

    public long count(String tenantId) {
        return repository.countByTenantId(tenantId);
    }

    public long countByStatus(String tenantId, CertificateEntry.CertStatus status) {
        return repository.countByTenantIdAndStatus(tenantId, status);
    }

    // ── Internal helpers ────────────────────────────────────────────────

    X509Certificate parsePem(String pem) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid PEM certificate: " + e.getMessage());
        }
    }

    private void populateX509Fields(CertificateEntry entry, X509Certificate x509) {
        entry.setSubjectDn(x509.getSubjectX500Principal().getName());
        entry.setIssuerDn(x509.getIssuerX500Principal().getName());
        entry.setSerialNumber(x509.getSerialNumber().toString(16));
        entry.setNotBefore(x509.getNotBefore().getTime());
        entry.setNotAfter(x509.getNotAfter().getTime());
    }

    private boolean isExpired(X509Certificate x509) {
        return x509.getNotAfter().getTime() < System.currentTimeMillis();
    }

    private CertificateEntry findByIdOrThrow(String tenantId, String id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Certificate not found: " + id));
    }

    private void checkDuplicateName(String tenantId, String name, String excludeId) {
        repository.findByTenantIdAndName(tenantId, name).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A certificate with name '" + name + "' already exists for this tenant.");
            }
        });
    }

    private void encryptPrivateKey(CertificateEntry entry) {
        if (entry.getPrivateKeyPem() != null && !entry.getPrivateKeyPem().isBlank()) {
            entry.setPrivateKeyPem(encryptionService.encrypt(entry.getPrivateKeyPem()));
        }
    }

    private void maskPrivateKey(CertificateEntry entry) {
        entry.setPrivateKeyPem(null);
    }

    private void decryptPrivateKey(CertificateEntry entry) {
        if (entry.getPrivateKeyPem() != null && !entry.getPrivateKeyPem().isBlank()) {
            try {
                entry.setPrivateKeyPem(encryptionService.decrypt(entry.getPrivateKeyPem()));
            } catch (Exception e) {
                log.warn("Failed to decrypt private key for cert [{}]", entry.getId());
            }
        }
    }
}
