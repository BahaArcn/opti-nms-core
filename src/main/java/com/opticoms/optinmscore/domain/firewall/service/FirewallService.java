package com.opticoms.optinmscore.domain.firewall.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import com.opticoms.optinmscore.domain.firewall.repository.FirewallRuleRepository;
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
public class FirewallService {

    private final FirewallRuleRepository repository;
    private final IptablesExecutor iptablesExecutor;

    @Audited(action = AuditAction.CREATE, entityType = "FirewallRule")
    public FirewallRule createRule(String tenantId, FirewallRule rule) {
        rule.setTenantId(tenantId);
        rule.setRuleStatus(FirewallRule.RuleStatus.PENDING);
        rule.setLastError(null);
        return repository.save(rule);
    }

    public FirewallRule getRuleById(String tenantId, String ruleId) {
        return repository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Firewall rule not found: " + ruleId));
    }

    public Page<FirewallRule> listRules(String tenantId, Pageable pageable) {
        return repository.findByTenantIdOrderByPriorityAsc(tenantId, pageable);
    }

    public List<FirewallRule> listRulesByChain(String tenantId, FirewallRule.Chain chain) {
        return repository.findByTenantIdAndChainOrderByPriorityAsc(tenantId, chain);
    }

    public List<FirewallRule> listRulesByStatus(String tenantId, FirewallRule.RuleStatus status) {
        return repository.findByTenantIdAndRuleStatusOrderByPriorityAsc(tenantId, status);
    }

    public List<FirewallRule> listEnabledRules(String tenantId) {
        return repository.findByTenantIdAndEnabledTrueOrderByPriorityAsc(tenantId);
    }

    @Audited(action = AuditAction.UPDATE, entityType = "FirewallRule")
    public FirewallRule updateRule(String tenantId, String ruleId, FirewallRule updated) {
        FirewallRule existing = getRuleById(tenantId, ruleId);

        if (existing.getRuleStatus() == FirewallRule.RuleStatus.APPLIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot update an applied rule. Remove it first, then update.");
        }

        existing.setChain(updated.getChain());
        existing.setProtocol(updated.getProtocol());
        existing.setSourceIp(updated.getSourceIp());
        existing.setDestinationIp(updated.getDestinationIp());
        existing.setSourcePort(updated.getSourcePort());
        existing.setDestinationPort(updated.getDestinationPort());
        existing.setAction(updated.getAction());
        existing.setInterfaceName(updated.getInterfaceName());
        existing.setOutInterfaceName(updated.getOutInterfaceName());
        existing.setDescription(updated.getDescription());
        existing.setPriority(updated.getPriority());
        existing.setEnabled(updated.isEnabled());
        existing.setRuleStatus(FirewallRule.RuleStatus.PENDING);
        existing.setLastError(null);

        return repository.save(existing);
    }

    @Audited(action = AuditAction.DELETE, entityType = "FirewallRule")
    public void deleteRule(String tenantId, String ruleId) {
        FirewallRule rule = getRuleById(tenantId, ruleId);

        if (rule.getRuleStatus() == FirewallRule.RuleStatus.APPLIED) {
            removeRuleFromOs(tenantId, ruleId);
        }

        repository.delete(rule);
    }

    @Audited(action = AuditAction.APPLY, entityType = "FirewallRule")
    public FirewallRule applyRuleToOs(String tenantId, String ruleId) {
        FirewallRule rule = getRuleById(tenantId, ruleId);

        if (!rule.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot apply a disabled rule. Enable it first.");
        }

        if (rule.getRuleStatus() == FirewallRule.RuleStatus.APPLIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rule is already applied.");
        }

        String cmd = iptablesExecutor.buildCommandString(rule, IptablesExecutor.Operation.APPEND);
        log.info("Applying firewall rule [{}]: {}", ruleId, cmd);

        IptablesExecutor.ExecResult result = iptablesExecutor.execute(rule, IptablesExecutor.Operation.APPEND);

        if (result.isSuccess()) {
            rule.setRuleStatus(FirewallRule.RuleStatus.APPLIED);
            rule.setLastError(null);
        } else {
            rule.setRuleStatus(FirewallRule.RuleStatus.FAILED);
            rule.setLastError(result.stderr());
        }

        return repository.save(rule);
    }

    @Audited(action = AuditAction.REMOVE, entityType = "FirewallRule")
    public FirewallRule removeRuleFromOs(String tenantId, String ruleId) {
        FirewallRule rule = getRuleById(tenantId, ruleId);

        if (rule.getRuleStatus() != FirewallRule.RuleStatus.APPLIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Rule is not currently applied (status: " + rule.getRuleStatus() + ").");
        }

        String cmd = iptablesExecutor.buildCommandString(rule, IptablesExecutor.Operation.DELETE);
        log.info("Removing firewall rule [{}]: {}", ruleId, cmd);

        IptablesExecutor.ExecResult result = iptablesExecutor.execute(rule, IptablesExecutor.Operation.DELETE);

        if (result.isSuccess()) {
            rule.setRuleStatus(FirewallRule.RuleStatus.REMOVED);
            rule.setLastError(null);
        } else {
            rule.setRuleStatus(FirewallRule.RuleStatus.FAILED);
            rule.setLastError(result.stderr());
        }

        return repository.save(rule);
    }

    public long countRules(String tenantId) {
        return repository.countByTenantId(tenantId);
    }

    public long countByStatus(String tenantId, FirewallRule.RuleStatus status) {
        return repository.countByTenantIdAndRuleStatus(tenantId, status);
    }
}
