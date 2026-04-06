package com.opticoms.optinmscore.domain.multitenant.repository;

import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode.SlaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlaveNodeRepository extends MongoRepository<SlaveNode, String> {

    Page<SlaveNode> findByTenantId(String tenantId, Pageable pageable);

    Optional<SlaveNode> findByTenantIdAndSlaveAddress(String tenantId, String slaveAddress);

    List<SlaveNode> findByTenantIdAndStatus(String tenantId, SlaveStatus status);

    Page<SlaveNode> findByTenantIdAndStatus(String tenantId, SlaveStatus status, Pageable pageable);

    long countByTenantId(String tenantId);
}
