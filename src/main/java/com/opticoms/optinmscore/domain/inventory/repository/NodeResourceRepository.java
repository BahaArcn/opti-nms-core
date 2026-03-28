package com.opticoms.optinmscore.domain.inventory.repository;

import com.opticoms.optinmscore.domain.inventory.model.NodeResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NodeResourceRepository extends MongoRepository<NodeResource, String> {

    Page<NodeResource> findByTenantId(String tenantId, Pageable pageable);

    Optional<NodeResource> findByTenantIdAndNodeId(String tenantId, String nodeId);

    long countByTenantId(String tenantId);
}
