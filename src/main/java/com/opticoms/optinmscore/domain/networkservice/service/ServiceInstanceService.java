package com.opticoms.optinmscore.domain.networkservice.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceInstance;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceStatus;
import com.opticoms.optinmscore.domain.networkservice.repository.ServiceInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceInstanceService {

    private final ServiceInstanceRepository repository;

    @Audited(action = AuditAction.CREATE, entityType = "ServiceInstance")
    public ServiceInstance create(String tenantId, String networkId, ServiceInstance instance) {
        instance.setTenantId(tenantId);
        instance.setNetworkId(networkId);
        log.info("Creating service instance: name={}, network={}, tenant={}", instance.getName(), networkId, tenantId);
        return repository.save(instance);
    }

    public Page<ServiceInstance> listByNetwork(String tenantId, String networkId, Pageable pageable) {
        return repository.findByTenantIdAndNetworkId(tenantId, networkId, pageable);
    }

    public ServiceInstance getById(String tenantId, String id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Service instance not found: " + id));
    }

    public List<ServiceInstance> listByTenant(String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @Audited(action = AuditAction.UPDATE, entityType = "ServiceInstance")
    public ServiceInstance update(String tenantId, String id, ServiceInstance updated) {
        ServiceInstance existing = getById(tenantId, id);

        updated.setId(existing.getId());
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setTenantId(tenantId);
        updated.setNetworkId(existing.getNetworkId());
        if (updated.getHealthCheckUrl() == null) {
            updated.setHealthCheckUrl(existing.getHealthCheckUrl());
        }
        updated.setStatus(existing.getStatus());
        updated.setStatusMessage(existing.getStatusMessage());
        updated.setLastHealthCheck(existing.getLastHealthCheck());

        log.info("Updating service instance [{}]: name={}", id, updated.getName());
        return repository.save(updated);
    }

    @Audited(action = AuditAction.DELETE, entityType = "ServiceInstance")
    public void delete(String tenantId, String id) {
        ServiceInstance instance = getById(tenantId, id);
        log.info("Deleting service instance [{}]: name={}", id, instance.getName());
        repository.delete(instance);
    }

    public void updateStatus(String id, ServiceStatus status, String message) {
        repository.findById(id).ifPresent(instance -> {
            instance.setStatus(status);
            instance.setStatusMessage(message);
            instance.setLastHealthCheck(System.currentTimeMillis());
            repository.save(instance);
        });
    }

    public long countByNetwork(String tenantId, String networkId) {
        return repository.countByTenantIdAndNetworkId(tenantId, networkId);
    }
}
