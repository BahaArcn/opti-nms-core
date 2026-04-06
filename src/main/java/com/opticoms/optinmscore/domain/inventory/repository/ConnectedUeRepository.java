package com.opticoms.optinmscore.domain.inventory.repository;

import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectedUeRepository extends MongoRepository<ConnectedUe, String> {

    List<ConnectedUe> findByTenantId(String tenantId);

    Page<ConnectedUe> findByTenantId(String tenantId, Pageable pageable);

    Optional<ConnectedUe> findByTenantIdAndImsi(String tenantId, String imsi);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, ConnectedUe.UeStatus status);

    List<ConnectedUe> findByTenantIdAndImsiIn(String tenantId, List<String> imsiList);
}
