package com.opticoms.optinmscore.domain.tenant.repository;

import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends MongoRepository<Tenant, String> {

    Optional<Tenant> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);

    List<Tenant> findByActiveTrue();
}
