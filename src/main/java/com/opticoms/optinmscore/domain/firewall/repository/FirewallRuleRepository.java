package com.opticoms.optinmscore.domain.firewall.repository;

import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FirewallRuleRepository extends MongoRepository<FirewallRule, String> {

    Page<FirewallRule> findByTenantIdOrderByPriorityAsc(String tenantId, Pageable pageable);

    List<FirewallRule> findByTenantIdAndEnabledTrueOrderByPriorityAsc(String tenantId);

    List<FirewallRule> findByTenantIdAndChainOrderByPriorityAsc(String tenantId, FirewallRule.Chain chain);

    List<FirewallRule> findByTenantIdAndRuleStatusOrderByPriorityAsc(String tenantId, FirewallRule.RuleStatus status);

    Optional<FirewallRule> findByIdAndTenantId(String id, String tenantId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndRuleStatus(String tenantId, FirewallRule.RuleStatus status);
}
