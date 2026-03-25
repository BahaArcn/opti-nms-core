package com.opticoms.optinmscore.domain.network.repository;

import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmfConfigRepository extends MongoRepository<AmfConfig, String> {

    Optional<AmfConfig> findByTenantId(String tenantId);
}