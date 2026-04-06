package com.opticoms.optinmscore.domain.tenant.service;

import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.repository.TenantRepository;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final EncryptionService encryptionService;

    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.existsByTenantId(tenant.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tenant already exists: " + tenant.getTenantId());
        }
        encryptUri(tenant);
        log.info("Creating tenant: id={}, name={}", tenant.getTenantId(), tenant.getName());
        Tenant saved = tenantRepository.save(tenant);
        decryptUri(saved);
        return saved;
    }

    public Tenant getTenant(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant not found: " + tenantId));
        decryptUri(tenant);
        return tenant;
    }

    public Tenant getTenantById(String id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant not found with id: " + id));
        decryptUri(tenant);
        return tenant;
    }

    public List<Tenant> listTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        tenants.forEach(this::decryptUri);
        return tenants;
    }

    public List<Tenant> getActiveTenants() {
        List<Tenant> tenants = tenantRepository.findByActiveTrue();
        tenants.forEach(this::decryptUri);
        return tenants;
    }

    public Tenant updateTenant(String tenantId, Tenant update) {
        Tenant existing = getTenant(tenantId);
        existing.setName(update.getName());
        existing.setAmfUrl(update.getAmfUrl());
        existing.setSmfUrl(update.getSmfUrl());
        existing.setOpen5gsMongoUri(update.getOpen5gsMongoUri());
        existing.setUpfMetricsUrl(update.getUpfMetricsUrl());
        if (update.isActive() != existing.isActive()) {
            existing.setActive(update.isActive());
        }
        encryptUri(existing);
        log.info("Updating tenant: id={}", tenantId);
        Tenant saved = tenantRepository.save(existing);
        decryptUri(saved);
        return saved;
    }

    public Tenant deactivateTenant(String tenantId) {
        Tenant tenant = getTenant(tenantId);
        encryptUri(tenant);
        tenant.setActive(false);
        log.info("Deactivating tenant: id={}", tenantId);
        Tenant saved = tenantRepository.save(tenant);
        decryptUri(saved);
        return saved;
    }

    /**
     * Permanently deletes a tenant document. Used exclusively as a compensating
     * action when onboarding fails after tenant creation but before admin user
     * creation completes. Do NOT use for normal tenant removal -- use
     * {@link #deactivateTenant(String)} instead.
     */
    public void hardDeleteTenant(String tenantId) {
        tenantRepository.findByTenantId(tenantId).ifPresent(tenant -> {
            tenantRepository.delete(tenant);
            log.info("Hard-deleted tenant {} (onboarding compensation)", tenantId);
        });
    }

    private void encryptUri(Tenant tenant) {
        String uri = tenant.getOpen5gsMongoUri();
        if (uri != null && !uri.isBlank()) {
            tenant.setOpen5gsMongoUri(encryptionService.encrypt(uri));
        }
    }

    private void decryptUri(Tenant tenant) {
        String uri = tenant.getOpen5gsMongoUri();
        if (uri != null && !uri.isBlank()) {
            try {
                tenant.setOpen5gsMongoUri(encryptionService.decrypt(uri));
            } catch (Exception e) {
                log.warn("Could not decrypt open5gsMongoUri for tenant {} — may be plaintext (pre-migration)",
                        tenant.getTenantId());
            }
        }
    }
}
