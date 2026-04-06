package com.opticoms.optinmscore.domain.network.repository;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalConfigRepository extends MongoRepository<GlobalConfig, String> {

    Optional<GlobalConfig> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);
}
