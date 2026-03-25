package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NetworkConfigService {

    // Dependency Injection (Bağımlılık Enjeksiyonu)
    // @RequiredArgsConstructor sayesinde Spring bu repository'yi bizim için otomatik olarak "new"ler.
    private final GlobalConfigRepository globalConfigRepository;

    /**
     * İş Kuralı 1: Müşterinin ayarını kaydet veya zaten varsa GÜNCELLE.
     */
    @Audited(action = AuditAction.UPDATE, entityType = "GlobalConfig")
    public GlobalConfig saveOrUpdateGlobalConfig(String tenantId, GlobalConfig newConfig) {

        // 1. Veritabanında bu müşteriye (tenant) ait önceden kaydedilmiş bir ayar var mı? Kutuyu (Optional) getir.
        Optional<GlobalConfig> existingConfigOpt = globalConfigRepository.findByTenantId(tenantId);

        if (existingConfigOpt.isPresent()) {
            GlobalConfig existingConfig = existingConfigOpt.get();
            newConfig.setId(existingConfig.getId());
            newConfig.setVersion(existingConfig.getVersion());
            newConfig.setCreatedAt(existingConfig.getCreatedAt());
            newConfig.setCreatedBy(existingConfig.getCreatedBy());
            newConfig.setMaxSupportedDevices(existingConfig.getMaxSupportedDevices());
            newConfig.setMaxSupportedGNBs(existingConfig.getMaxSupportedGNBs());
        }

        // Tenant ID'sini güvenlik için biz kodla basıyoruz (Kullanıcı dışarıdan hackleyip başka tenant'a veri yazamasın diye)
        newConfig.setTenantId(tenantId);

        // 2. Repository'ye "Al bunu kaydet (veya güncelle)" diyoruz.
        return globalConfigRepository.save(newConfig);
    }

    /**
     * İş Kuralı 2: Müşterinin ayarını getir. Yoksa hata fırlat.
     */
    public GlobalConfig getGlobalConfig(String tenantId) {

        // Kutuyu aç. Eğer kutu boşsa (ayar yoksa) direkt kodun çalışmasını durdur ve Hata fırlat!
        return globalConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Global Configuration not found for tenant: " + tenantId));
    }
}