package com.opticoms.optinmscore.domain.network.repository;

import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UpfConfigRepository extends MongoRepository<UpfConfig, String> {
    Optional<UpfConfig> findByTenantId(String tenantId);
}