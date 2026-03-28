package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import com.opticoms.optinmscore.domain.network.repository.SmfConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmfConfigService {

    private final SmfConfigRepository smfConfigRepository;
    private final GlobalConfigRepository globalConfigRepository;

    @Audited(action = AuditAction.UPDATE, entityType = "SmfConfig")
    public SmfConfig saveOrUpdateSmfConfig(String tenantId, SmfConfig newConfig) {
        validateTunInterfaceReferences(tenantId, newConfig);

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

    private void validateTunInterfaceReferences(String tenantId, SmfConfig config) {
        if (config.getApnList() == null || config.getApnList().isEmpty()) {
            return;
        }

        Optional<GlobalConfig> globalOpt = globalConfigRepository.findByTenantId(tenantId);
        Set<String> validTunInterfaces = globalOpt
                .map(g -> g.getUeIpPoolList() != null
                        ? g.getUeIpPoolList().stream()
                            .map(GlobalConfig.UeIpPool::getTunInterface)
                            .collect(Collectors.toSet())
                        : Collections.<String>emptySet())
                .orElse(Collections.emptySet());

        for (SmfConfig.ApnDnn dnn : config.getApnList()) {
            if (dnn.getTunInterface() != null && !validTunInterfaces.contains(dnn.getTunInterface())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "tunInterface '" + dnn.getTunInterface() + "' not found in UeIpPool list");
            }
        }
    }
}