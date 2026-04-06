package com.opticoms.optinmscore.domain.networkservice.repository;

import com.opticoms.optinmscore.domain.networkservice.model.ServiceInstance;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceInstanceRepository extends MongoRepository<ServiceInstance, String> {

    Page<ServiceInstance> findByTenantIdAndNetworkId(String tenantId, String networkId, Pageable pageable);

    Optional<ServiceInstance> findByIdAndTenantId(String id, String tenantId);

    List<ServiceInstance> findByTenantId(String tenantId);

    List<ServiceInstance> findByTenantIdAndNetworkId(String tenantId, String networkId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, ServiceStatus status);

    long countByTenantIdAndNetworkIdAndStatus(String tenantId, String networkId, ServiceStatus status);

    long countByTenantIdAndNetworkId(String tenantId, String networkId);

    void deleteByTenantIdAndNetworkId(String tenantId, String networkId);
}
