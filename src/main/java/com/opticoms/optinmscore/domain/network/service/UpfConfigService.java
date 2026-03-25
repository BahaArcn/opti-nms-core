package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import com.opticoms.optinmscore.domain.network.repository.UpfConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpfConfigService {

    private final UpfConfigRepository upfConfigRepository;

    @Audited(action = AuditAction.UPDATE, entityType = "UpfConfig")
    public UpfConfig saveOrUpdateUpfConfig(String tenantId, UpfConfig newConfig) {
        Optional<UpfConfig> existingOpt = upfConfigRepository.findByTenantId(tenantId);

        if (existingOpt.isPresent()) {
            UpfConfig existing = existingOpt.get();
            newConfig.setId(existing.getId());
            newConfig.setVersion(existing.getVersion());
            newConfig.setCreatedAt(existing.getCreatedAt());
            newConfig.setCreatedBy(existing.getCreatedBy());
        }
        newConfig.setTenantId(tenantId);
        return upfConfigRepository.save(newConfig);
    }

    public UpfConfig getUpfConfig(String tenantId) {
        return upfConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "UPF Configuration not found for tenant: " + tenantId));
    }
}