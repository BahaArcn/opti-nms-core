package com.opticoms.optinmscore.domain.apn.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.apn.model.ApnProfile;
import com.opticoms.optinmscore.domain.apn.repository.ApnProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApnProfileService {

    private final ApnProfileRepository repository;

    @Audited(action = AuditAction.CREATE, entityType = "ApnProfile")
    public ApnProfile create(String tenantId, ApnProfile profile) {
        validateSlice(profile);
        checkDuplicate(tenantId, profile.getDnn(), profile.getSst(), null);

        profile.setTenantId(tenantId);
        log.info("Creating APN/DNN profile: dnn={}, sst={}, tenant={}", profile.getDnn(), profile.getSst(), tenantId);
        return repository.save(profile);
    }

    public ApnProfile getById(String tenantId, String id) {
        return findByIdOrThrow(tenantId, id);
    }

    public Page<ApnProfile> list(String tenantId, Pageable pageable) {
        return repository.findByTenantId(tenantId, pageable);
    }

    public List<ApnProfile> listByStatus(String tenantId, ApnProfile.ProfileStatus status) {
        return repository.findByTenantIdAndStatus(tenantId, status);
    }

    public List<ApnProfile> listBySst(String tenantId, Integer sst) {
        return repository.findByTenantIdAndSst(tenantId, sst);
    }

    public List<ApnProfile> listEnabled(String tenantId) {
        return repository.findByTenantIdAndEnabledTrue(tenantId);
    }

    @Audited(action = AuditAction.UPDATE, entityType = "ApnProfile")
    public ApnProfile update(String tenantId, String id, ApnProfile updated) {
        ApnProfile existing = findByIdOrThrow(tenantId, id);

        if (existing.getStatus() == ApnProfile.ProfileStatus.DEPRECATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot update a deprecated APN profile. Create a new one instead.");
        }

        validateSlice(updated);
        checkDuplicate(tenantId, updated.getDnn(), updated.getSst(), id);

        existing.setDnn(updated.getDnn());
        existing.setSst(updated.getSst());
        existing.setSd(updated.getSd());
        existing.setPduSessionType(updated.getPduSessionType());
        existing.setQos(updated.getQos());
        existing.setSessionAmbr(updated.getSessionAmbr());
        existing.setEnabled(updated.isEnabled());
        existing.setDescription(updated.getDescription());
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }

        log.info("Updating APN/DNN profile [{}]: dnn={}, sst={}", id, existing.getDnn(), existing.getSst());
        return repository.save(existing);
    }

    @Audited(action = AuditAction.DELETE, entityType = "ApnProfile")
    public void delete(String tenantId, String id) {
        ApnProfile profile = findByIdOrThrow(tenantId, id);
        log.info("Deleting APN/DNN profile [{}]: dnn={}", id, profile.getDnn());
        repository.delete(profile);
    }

    @Audited(action = AuditAction.DEPRECATE, entityType = "ApnProfile")
    public ApnProfile deprecate(String tenantId, String id) {
        ApnProfile profile = findByIdOrThrow(tenantId, id);

        if (profile.getStatus() == ApnProfile.ProfileStatus.DEPRECATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profile is already deprecated.");
        }

        profile.setStatus(ApnProfile.ProfileStatus.DEPRECATED);
        profile.setEnabled(false);
        log.info("Deprecating APN/DNN profile [{}]: dnn={}", id, profile.getDnn());
        return repository.save(profile);
    }

    public long count(String tenantId) {
        return repository.countByTenantId(tenantId);
    }

    public long countByStatus(String tenantId, ApnProfile.ProfileStatus status) {
        return repository.countByTenantIdAndStatus(tenantId, status);
    }

    private ApnProfile findByIdOrThrow(String tenantId, String id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "APN/DNN profile not found: " + id));
    }

    private void validateSlice(ApnProfile profile) {
        if (profile.getSd() != null && !profile.getSd().matches("^[0-9a-fA-F]{6}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "SD must be a 6-character hex string (e.g. '000001'). Got: " + profile.getSd());
        }
    }

    private void checkDuplicate(String tenantId, String dnn, Integer sst, String excludeId) {
        repository.findByTenantIdAndDnnAndSst(tenantId, dnn, sst).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An APN profile with dnn='" + dnn + "' and sst=" + sst
                                + " already exists for this tenant.");
            }
        });
    }
}
