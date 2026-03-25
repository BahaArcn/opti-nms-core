package com.opticoms.optinmscore.domain.inventory.repository;

import com.opticoms.optinmscore.domain.inventory.model.PduSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PduSessionRepository extends MongoRepository<PduSession, String> {

    List<PduSession> findByTenantId(String tenantId);

    Optional<PduSession> findByTenantIdAndSessionId(String tenantId, String sessionId);

    Page<PduSession> findByTenantId(String tenantId, Pageable pageable);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, PduSession.SessionStatus status);

    long countByTenantIdAndDnn(String tenantId, String dnn);
}
