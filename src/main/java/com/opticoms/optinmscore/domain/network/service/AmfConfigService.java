package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.repository.AmfConfigRepository;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AmfConfigService {

    private final AmfConfigRepository amfConfigRepository;
    private final GlobalConfigRepository globalConfigRepository;

    @Audited(action = AuditAction.UPDATE, entityType = "AmfConfig")
    public AmfConfig saveOrUpdateAmfConfig(String tenantId, AmfConfig newConfig) {

        GlobalConfig.NetworkMode mode = resolveNetworkMode(tenantId);
        validateForNetworkMode(newConfig, mode);

        Optional<AmfConfig> existingConfigOpt = amfConfigRepository.findByTenantId(tenantId);

        if (existingConfigOpt.isPresent()) {
            AmfConfig existingConfig = existingConfigOpt.get();
            newConfig.setId(existingConfig.getId());
            newConfig.setVersion(existingConfig.getVersion());
            newConfig.setCreatedAt(existingConfig.getCreatedAt());
            newConfig.setCreatedBy(existingConfig.getCreatedBy());
        }

        newConfig.setTenantId(tenantId);

        if (newConfig.getSupportedTais() != null) {
            for (AmfConfig.Tai tai : newConfig.getSupportedTais()) {
                if (tai.getTacEnd() != 0 && tai.getTacEnd() < tai.getTac()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "TAI range invalid: tacEnd (" + tai.getTacEnd() + ") must be >= tac (" + tai.getTac() + ")");
                }
            }
        }

        return amfConfigRepository.save(newConfig);
    }

    public AmfConfig getAmfConfig(String tenantId) {
        return amfConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "AMF Configuration not found for tenant: " + tenantId));
    }

    private GlobalConfig.NetworkMode resolveNetworkMode(String tenantId) {
        return globalConfigRepository.findByTenantId(tenantId)
                .map(GlobalConfig::getNetworkMode)
                .orElse(GlobalConfig.NetworkMode.ONLY_5G);
    }

    private void validateForNetworkMode(AmfConfig config, GlobalConfig.NetworkMode mode) {
        boolean needs5g = mode == GlobalConfig.NetworkMode.ONLY_5G
                || mode == GlobalConfig.NetworkMode.HYBRID_4G_5G;
        boolean needs4g = mode == GlobalConfig.NetworkMode.ONLY_4G
                || mode == GlobalConfig.NetworkMode.HYBRID_4G_5G;

        List<String> errors = new ArrayList<>();

        if (needs5g) {
            if (config.getAmfName() == null || config.getAmfName().isBlank()) {
                errors.add("amfName is required in " + mode + " mode");
            }
            if (config.getAmfId() == null) {
                errors.add("amfId (region/set/pointer) is required in " + mode + " mode");
            }
            if (config.getSupportedSlices() == null || config.getSupportedSlices().isEmpty()) {
                errors.add("supportedSlices is required in " + mode + " mode");
            }
            if (config.getNasTimers5g() == null) {
                errors.add("nasTimers5g is required in " + mode + " mode");
            }
        }

        if (needs4g) {
            if (config.getMmeName() == null || config.getMmeName().isBlank()) {
                errors.add("mmeName is required in " + mode + " mode");
            }
            if (config.getMmeId() == null) {
                errors.add("mmeId (MMEGI/MMEC) is required in " + mode + " mode");
            }
            if (config.getNasTimers4g() == null) {
                errors.add("nasTimers4g is required in " + mode + " mode");
            }
        }

        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Validation failed for " + mode + " mode: " + String.join("; ", errors));
        }
    }
}