package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import com.opticoms.optinmscore.domain.network.repository.UpfConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpfConfigService {

    private final UpfConfigRepository upfConfigRepository;
    private final GlobalConfigRepository globalConfigRepository;

    @Audited(action = AuditAction.UPDATE, entityType = "UpfConfig")
    public UpfConfig saveOrUpdateUpfConfig(String tenantId, UpfConfig newConfig) {
        validateForNetworkMode(tenantId, newConfig);

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

    private void validateForNetworkMode(String tenantId, UpfConfig config) {
        GlobalConfig.NetworkMode mode = globalConfigRepository.findByTenantId(tenantId)
                .map(GlobalConfig::getNetworkMode)
                .orElse(GlobalConfig.NetworkMode.ONLY_5G);

        boolean needs5g = mode == GlobalConfig.NetworkMode.ONLY_5G
                || mode == GlobalConfig.NetworkMode.HYBRID_4G_5G;
        boolean needs4g = mode == GlobalConfig.NetworkMode.ONLY_4G
                || mode == GlobalConfig.NetworkMode.HYBRID_4G_5G;

        List<String> errors = new ArrayList<>();

        if (needs5g && isBlank(config.getN3InterfaceIp())) {
            errors.add("n3InterfaceIp is required in " + mode + " mode");
        }
        if (needs4g && isBlank(config.getS1uInterfaceIp())) {
            errors.add("s1uInterfaceIp is required in " + mode + " mode");
        }

        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Validation failed for " + mode + " mode: " + String.join("; ", errors));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}