package com.opticoms.optinmscore.domain.apn.repository;

import com.opticoms.optinmscore.domain.apn.model.ApnProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApnProfileRepository extends MongoRepository<ApnProfile, String> {

    Page<ApnProfile> findByTenantId(String tenantId, Pageable pageable);

    Optional<ApnProfile> findByIdAndTenantId(String id, String tenantId);

    Optional<ApnProfile> findByTenantIdAndDnnAndSst(String tenantId, String dnn, Integer sst);

    Optional<ApnProfile> findFirstByTenantIdAndDnnAndEnabledTrue(String tenantId, String dnn);

    List<ApnProfile> findByTenantIdAndStatus(String tenantId, ApnProfile.ProfileStatus status);

    List<ApnProfile> findByTenantIdAndSst(String tenantId, Integer sst);

    List<ApnProfile> findByTenantIdAndEnabledTrue(String tenantId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, ApnProfile.ProfileStatus status);
}
