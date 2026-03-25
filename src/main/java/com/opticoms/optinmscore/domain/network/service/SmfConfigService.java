package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import com.opticoms.optinmscore.domain.network.repository.SmfConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SmfConfigService {

    private final SmfConfigRepository smfConfigRepository;

    @Audited(action = AuditAction.UPDATE, entityType = "SmfConfig")
    public SmfConfig saveOrUpdateSmfConfig(String tenantId, SmfConfig newConfig) {
        Optional<SmfConfig> existingOpt = smfConfigRepository.findByTenantId(tenantId);

        if (existingOpt.isPresent()) {
            SmfConfig existing = existingOpt.get();
            newConfig.setId(existing.getId());
            newConfig.setVersion(existing.getVersion());
            newConfig.setCreatedAt(existing.getCreatedAt());
            newConfig.setCreatedBy(existing.getCreatedBy());
        }
        newConfig.setTenantId(tenantId);
        return smfConfigRepository.save(newConfig);
    }

    public SmfConfig getSmfConfig(String tenantId) {
        return smfConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "SMF Configuration not found for tenant: " + tenantId));
    }
}