package com.opticoms.optinmscore.domain.edgelocation.repository;

import com.opticoms.optinmscore.domain.edgelocation.model.EdgeLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EdgeLocationRepository extends MongoRepository<EdgeLocation, String> {

    Page<EdgeLocation> findByTenantId(String tenantId, Pageable pageable);

    Optional<EdgeLocation> findByIdAndTenantId(String id, String tenantId);

    long countByTenantId(String tenantId);

    boolean existsByTenantIdAndName(String tenantId, String name);
}
