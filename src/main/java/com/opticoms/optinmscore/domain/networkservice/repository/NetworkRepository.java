package com.opticoms.optinmscore.domain.networkservice.repository;

import com.opticoms.optinmscore.domain.networkservice.model.Network;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkRepository extends MongoRepository<Network, String> {

    Page<Network> findByTenantId(String tenantId, Pageable pageable);

    Optional<Network> findByIdAndTenantId(String id, String tenantId);

    long countByTenantId(String tenantId);

    boolean existsByTenantIdAndName(String tenantId, String name);
}
