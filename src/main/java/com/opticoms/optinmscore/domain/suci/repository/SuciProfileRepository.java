package com.opticoms.optinmscore.domain.suci.repository;

import com.opticoms.optinmscore.domain.suci.model.SuciProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuciProfileRepository extends MongoRepository<SuciProfile, String> {

    Page<SuciProfile> findByTenantId(String tenantId, Pageable pageable);

    Optional<SuciProfile> findByIdAndTenantId(String id, String tenantId);

    List<SuciProfile> findByTenantIdAndKeyStatus(String tenantId, SuciProfile.KeyStatus keyStatus);

    List<SuciProfile> findByTenantIdAndProtectionScheme(String tenantId, SuciProfile.ProtectionScheme scheme);

    Optional<SuciProfile> findByTenantIdAndHomeNetworkPublicKeyIdAndProtectionScheme(
            String tenantId, Integer homeNetworkPublicKeyId, SuciProfile.ProtectionScheme scheme);

    long countByTenantId(String tenantId);

    long countByTenantIdAndKeyStatus(String tenantId, SuciProfile.KeyStatus keyStatus);
}
