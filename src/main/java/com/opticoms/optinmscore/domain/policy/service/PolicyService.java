package com.opticoms.optinmscore.domain.policy.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.policy.model.Policy;
import com.opticoms.optinmscore.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;

    @Audited(action = AuditAction.CREATE, entityType = "Policy")
    public Policy createPolicy(String tenantId, Policy policy) {
        if (policyRepository.existsByTenantIdAndName(tenantId, policy.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Policy with name '" + policy.getName() + "' already exists");
        }
        policy.setTenantId(tenantId);
        return policyRepository.save(policy);
    }

    public Page<Policy> listPolicies(String tenantId, Pageable pageable) {
        return policyRepository.findByTenantId(tenantId, pageable);
    }

    public Policy getPolicy(String tenantId, String id) {
        return policyRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Policy not found: " + id));
    }

    @Audited(action = AuditAction.UPDATE, entityType = "Policy")
    public Policy updatePolicy(String tenantId, String id, Policy updated) {
        Policy existing = getPolicy(tenantId, id);

        updated.setId(existing.getId());
        updated.setVersion(existing.getVersion());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setTenantId(tenantId);

        return policyRepository.save(updated);
    }

    @Audited(action = AuditAction.DELETE, entityType = "Policy")
    public void deletePolicy(String tenantId, String id) {
        Policy policy = getPolicy(tenantId, id);
        policyRepository.delete(policy);
    }

    public long countPolicies(String tenantId) {
        return policyRepository.countByTenantId(tenantId);
    }

    public boolean existsForTenant(String tenantId, String policyId) {
        return policyRepository.existsByIdAndTenantId(policyId, tenantId);
    }
}
