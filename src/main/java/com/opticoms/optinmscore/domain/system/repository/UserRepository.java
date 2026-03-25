package com.opticoms.optinmscore.domain.system.repository;

import com.opticoms.optinmscore.domain.system.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndTenantId(String username, String tenantId);

    Optional<User> findByIdAndTenantId(String id, String tenantId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByTenantIdAndUsername(String tenantId, String username);

    boolean existsByTenantIdAndEmail(String tenantId, String email);

    List<User> findByTenantId(String tenantId);

    Page<User> findByTenantId(String tenantId, Pageable pageable);

    long countByTenantId(String tenantId);
}