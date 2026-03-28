package com.opticoms.optinmscore.domain.tenant.service;

import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.repository.TenantRepository;
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

    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.existsByTenantId(tenant.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tenant already exists: " + tenant.getTenantId());
        }
        log.info("Creating tenant: id={}, name={}", tenant.getTenantId(), tenant.getName());
        return tenantRepository.save(tenant);
    }

    public Tenant getTenant(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant not found: " + tenantId));
    }

    public Tenant getTenantById(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant not found with id: " + id));
    }

    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    public List<Tenant> getActiveTenants() {
        return tenantRepository.findByActiveTrue();
    }

    public Tenant updateTenant(String tenantId, Tenant update) {
        Tenant existing = getTenant(tenantId);
        existing.setName(update.getName());
        existing.setAmfUrl(update.getAmfUrl());
        existing.setSmfUrl(update.getSmfUrl());
        existing.setOpen5gsMongoUri(update.getOpen5gsMongoUri());
        if (update.isActive() != existing.isActive()) {
            existing.setActive(update.isActive());
        }
        log.info("Updating tenant: id={}", tenantId);
        return tenantRepository.save(existing);
    }

    public Tenant deactivateTenant(String tenantId) {
        Tenant tenant = getTenant(tenantId);
        tenant.setActive(false);
        log.info("Deactivating tenant: id={}", tenantId);
        return tenantRepository.save(tenant);
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
}
