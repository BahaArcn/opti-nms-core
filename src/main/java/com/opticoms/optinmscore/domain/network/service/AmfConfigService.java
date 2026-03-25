package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import com.opticoms.optinmscore.domain.network.repository.AmfConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AmfConfigService {

    private final AmfConfigRepository amfConfigRepository;

    @Audited(action = AuditAction.UPDATE, entityType = "AmfConfig")
    public AmfConfig saveOrUpdateAmfConfig(String tenantId, AmfConfig newConfig) {

        Optional<AmfConfig> existingConfigOpt = amfConfigRepository.findByTenantId(tenantId);

        if (existingConfigOpt.isPresent()) {
            AmfConfig existingConfig = existingConfigOpt.get();
            newConfig.setId(existingConfig.getId());
            newConfig.setVersion(existingConfig.getVersion());
            newConfig.setCreatedAt(existingConfig.getCreatedAt());
            newConfig.setCreatedBy(existingConfig.getCreatedBy());
        }

        // Güvenlik: Tenant ID'yi dışarıdan gelen nesneye kodla basıyoruz.
        newConfig.setTenantId(tenantId);

        return amfConfigRepository.save(newConfig);
    }

    public AmfConfig getAmfConfig(String tenantId) {
        return amfConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "AMF Configuration not found for tenant: " + tenantId));
    }
}