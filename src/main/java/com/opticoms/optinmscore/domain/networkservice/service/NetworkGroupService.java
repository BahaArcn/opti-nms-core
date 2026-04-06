package com.opticoms.optinmscore.domain.networkservice.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.networkservice.model.Network;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceStatus;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkSummaryResponse;
import com.opticoms.optinmscore.domain.networkservice.repository.NetworkRepository;
import com.opticoms.optinmscore.domain.networkservice.repository.ServiceInstanceRepository;
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
public class NetworkGroupService {

    private final NetworkRepository networkRepository;
    private final ServiceInstanceRepository serviceInstanceRepository;

    @Audited(action = AuditAction.CREATE, entityType = "Network")
    public Network create(String tenantId, Network network) {
        if (networkRepository.existsByTenantIdAndName(tenantId, network.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Network with name '" + network.getName() + "' already exists");
        }
        network.setTenantId(tenantId);
        log.info("Creating network: name={}, tenant={}", network.getName(), tenantId);
        return networkRepository.save(network);
    }

    public Page<Network> list(String tenantId, Pageable pageable) {
        return networkRepository.findByTenantId(tenantId, pageable);
    }

    public Network getById(String tenantId, String id) {
        return networkRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Network not found: " + id));
    }

    @Audited(action = AuditAction.UPDATE, entityType = "Network")
    public Network update(String tenantId, String id, Network updated) {
        Network existing = getById(tenantId, id);

        if (!existing.getName().equals(updated.getName())
                && networkRepository.existsByTenantIdAndName(tenantId, updated.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Network with name '" + updated.getName() + "' already exists");
        }

        updated.setId(existing.getId());
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setTenantId(tenantId);

        log.info("Updating network [{}]: name={}", id, updated.getName());
        return networkRepository.save(updated);
    }

    @Audited(action = AuditAction.DELETE, entityType = "Network")
    public void delete(String tenantId, String id) {
        Network network = getById(tenantId, id);
        serviceInstanceRepository.deleteByTenantIdAndNetworkId(tenantId, id);
        log.info("Deleting network [{}] and its service instances: name={}", id, network.getName());
        networkRepository.delete(network);
    }

    public long count(String tenantId) {
        return networkRepository.countByTenantId(tenantId);
    }

    public NetworkSummaryResponse getSummary(String tenantId, String networkId) {
        Network network = getById(tenantId, networkId);
        long total = serviceInstanceRepository.countByTenantIdAndNetworkId(tenantId, networkId);
        long running = serviceInstanceRepository.countByTenantIdAndNetworkIdAndStatus(tenantId, networkId, ServiceStatus.RUNNING);
        long stopped = serviceInstanceRepository.countByTenantIdAndNetworkIdAndStatus(tenantId, networkId, ServiceStatus.STOPPED);
        long error = serviceInstanceRepository.countByTenantIdAndNetworkIdAndStatus(tenantId, networkId, ServiceStatus.ERROR);

        return NetworkSummaryResponse.builder()
                .networkId(networkId)
                .networkName(network.getName())
                .totalServices(total)
                .runningServices(running)
                .stoppedServices(stopped)
                .errorServices(error)
                .build();
    }
}
