package com.opticoms.optinmscore.domain.network.repository;

import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmfConfigRepository extends MongoRepository<SmfConfig, String> {
    Optional<SmfConfig> findByTenantId(String tenantId);
}