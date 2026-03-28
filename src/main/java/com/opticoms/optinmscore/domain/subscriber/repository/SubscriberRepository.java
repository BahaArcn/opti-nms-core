package com.opticoms.optinmscore.domain.subscriber.repository;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends MongoRepository<Subscriber, String> {

    Optional<Subscriber> findByImsiHashAndTenantId(String imsiHash, String tenantId);

    boolean existsByTenantIdAndImsiHash(String tenantId, String imsiHash);

    List<Subscriber> findByTenantId(String tenantId);

    Page<Subscriber> findByTenantId(String tenantId, Pageable pageable);

    long countByTenantId(String tenantId);

    Page<Subscriber> findByTenantIdAndLabelContainingIgnoreCase(String tenantId, String label, Pageable pageable);

    Optional<Subscriber> findByMsisdnHashAndTenantId(String msisdnHash, String tenantId);
}