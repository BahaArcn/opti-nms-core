package com.opticoms.optinmscore.domain.license.repository;

import com.opticoms.optinmscore.domain.license.model.License;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LicenseRepository extends MongoRepository<License, String> {

    Optional<License> findByTenantId(String tenantId);

    boolean existsByTenantId(String tenantId);
}
