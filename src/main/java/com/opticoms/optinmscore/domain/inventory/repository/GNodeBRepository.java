package com.opticoms.optinmscore.domain.inventory.repository;

import com.opticoms.optinmscore.domain.inventory.model.GNodeB;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GNodeBRepository extends MongoRepository<GNodeB, String> {

    List<GNodeB> findByTenantId(String tenantId);

    Page<GNodeB> findByTenantId(String tenantId, Pageable pageable);

    Optional<GNodeB> findByTenantIdAndGnbId(String tenantId, String gnbId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, GNodeB.ConnectionStatus status);
}
