package com.opticoms.optinmscore.domain.policy.repository;

import com.opticoms.optinmscore.domain.policy.model.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyRepository extends MongoRepository<Policy, String> {

    Page<Policy> findByTenantId(String tenantId, Pageable pageable);

    Optional<Policy> findByIdAndTenantId(String id, String tenantId);

    boolean existsByIdAndTenantId(String id, String tenantId);

    boolean existsByTenantIdAndName(String tenantId, String name);

    long countByTenantId(String tenantId);
}
