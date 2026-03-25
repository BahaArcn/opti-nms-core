package com.opticoms.optinmscore.domain.network.repository;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalConfigRepository extends MongoRepository<GlobalConfig, String> {

    // Spring Data Sihri: Sadece metodun adını yazıyoruz, kodunu Spring yazıyor!
    Optional<GlobalConfig> findByTenantId(String tenantId);

    // Eğer bir tenant'ın konfigürasyonu var mı diye kontrol etmek istersek:
    boolean existsByTenantId(String tenantId);
}