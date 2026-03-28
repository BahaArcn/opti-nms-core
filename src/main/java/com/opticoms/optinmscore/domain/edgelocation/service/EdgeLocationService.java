package com.opticoms.optinmscore.domain.edgelocation.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.edgelocation.model.EdgeLocation;
import com.opticoms.optinmscore.domain.edgelocation.repository.EdgeLocationRepository;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeLocationService {

    private final EdgeLocationRepository repository;
    private final LicenseService licenseService;

    @Audited(action = AuditAction.CREATE, entityType = "EdgeLocation")
    public EdgeLocation create(String tenantId, EdgeLocation edgeLocation) {
        licenseService.checkCanAddEdgeLocation(tenantId);
        if (repository.existsByTenantIdAndName(tenantId, edgeLocation.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Edge location with name '" + edgeLocation.getName() + "' already exists");
        }
        edgeLocation.setTenantId(tenantId);
        log.info("Creating edge location: name={}, tenant={}", edgeLocation.getName(), tenantId);
        return repository.save(edgeLocation);
    }

    public Page<EdgeLocation> list(String tenantId, Pageable pageable) {
        return repository.findByTenantId(tenantId, pageable);
    }

    public EdgeLocation getById(String tenantId, String id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Edge location not found: " + id));
    }

    @Audited(action = AuditAction.UPDATE, entityType = "EdgeLocation")
    public EdgeLocation update(String tenantId, String id, EdgeLocation updated) {
        EdgeLocation existing = getById(tenantId, id);

        if (!existing.getName().equals(updated.getName())
                && repository.existsByTenantIdAndName(tenantId, updated.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Edge location with name '" + updated.getName() + "' already exists");
        }

        updated.setId(existing.getId());
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setTenantId(tenantId);

        log.info("Updating edge location [{}]: name={}", id, updated.getName());
        return repository.save(updated);
    }

    @Audited(action = AuditAction.DELETE, entityType = "EdgeLocation")
    public void delete(String tenantId, String id) {
        EdgeLocation edgeLocation = getById(tenantId, id);
        log.info("Deleting edge location [{}]: name={}", id, edgeLocation.getName());
        repository.delete(edgeLocation);
    }

    public long count(String tenantId) {
        return repository.countByTenantId(tenantId);
    }
}
