package com.opticoms.optinmscore.domain.firewall.service;

import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import com.opticoms.optinmscore.domain.firewall.repository.FirewallRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirewallServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private FirewallRuleRepository repository;
    @Mock private IptablesExecutor iptablesExecutor;
    @InjectMocks private FirewallService service;

    private FirewallRule sampleRule;

    @BeforeEach
    void setUp() {
        sampleRule = new FirewallRule();
        sampleRule.setId("rule-1");
        sampleRule.setTenantId(TENANT);
        sampleRule.setChain(FirewallRule.Chain.INPUT);
        sampleRule.setProtocol(FirewallRule.Protocol.TCP);
        sampleRule.setDestinationPort(8080);
        sampleRule.setAction(FirewallRule.Action.ACCEPT);
        sampleRule.setDescription("Allow HTTP alt");
        sampleRule.setPriority(100);
        sampleRule.setEnabled(true);
        sampleRule.setRuleStatus(FirewallRule.RuleStatus.PENDING);
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Test
    void createRule_setsTenantAndPendingStatus() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FirewallRule input = new FirewallRule();
        input.setChain(FirewallRule.Chain.INPUT);
        input.setProtocol(FirewallRule.Protocol.TCP);
        input.setAction(FirewallRule.Action.DROP);
        input.setDescription("Block something");

        FirewallRule result = service.createRule(TENANT, input);

        assertThat(result.getTenantId()).isEqualTo(TENANT);
        assertThat(result.getRuleStatus()).isEqualTo(FirewallRule.RuleStatus.PENDING);

        ArgumentCaptor<FirewallRule> captor = ArgumentCaptor.forClass(FirewallRule.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT);
    }

    // ── Read ────────────────────────────────────────────────────────────

    @Test
    void getRuleById_found_returnsRule() {
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));

        FirewallRule result = service.getRuleById(TENANT, "rule-1");

        assertThat(result.getId()).isEqualTo("rule-1");
    }

    @Test
    void getRuleById_notFound_throws404() {
        when(repository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRuleById(TENANT, "missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void listRules_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findByTenantIdOrderByPriorityAsc(TENANT, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleRule)));

        Page<FirewallRule> result = service.listRules(TENANT, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listRulesByChain_delegatesToRepository() {
        when(repository.findByTenantIdAndChainOrderByPriorityAsc(TENANT, FirewallRule.Chain.INPUT))
                .thenReturn(List.of(sampleRule));

        List<FirewallRule> result = service.listRulesByChain(TENANT, FirewallRule.Chain.INPUT);

        assertThat(result).hasSize(1);
    }

    @Test
    void listEnabledRules_delegatesToRepository() {
        when(repository.findByTenantIdAndEnabledTrueOrderByPriorityAsc(TENANT))
                .thenReturn(List.of(sampleRule));

        List<FirewallRule> result = service.listEnabledRules(TENANT);

        assertThat(result).hasSize(1);
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Test
    void updateRule_pendingRule_updatesFields() {
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FirewallRule updated = new FirewallRule();
        updated.setChain(FirewallRule.Chain.OUTPUT);
        updated.setProtocol(FirewallRule.Protocol.UDP);
        updated.setDestinationPort(53);
        updated.setAction(FirewallRule.Action.DROP);
        updated.setDescription("Block DNS out");
        updated.setPriority(50);
        updated.setEnabled(true);

        FirewallRule result = service.updateRule(TENANT, "rule-1", updated);

        assertThat(result.getChain()).isEqualTo(FirewallRule.Chain.OUTPUT);
        assertThat(result.getProtocol()).isEqualTo(FirewallRule.Protocol.UDP);
        assertThat(result.getDestinationPort()).isEqualTo(53);
        assertThat(result.getRuleStatus()).isEqualTo(FirewallRule.RuleStatus.PENDING);
    }

    @Test
    void updateRule_appliedRule_throwsConflict() {
        sampleRule.setRuleStatus(FirewallRule.RuleStatus.APPLIED);
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));

        FirewallRule updated = new FirewallRule();

        assertThatThrownBy(() -> service.updateRule(TENANT, "rule-1", updated))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot update an applied rule");
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Test
    void deleteRule_pendingRule_deletesDirectly() {
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));

        service.deleteRule(TENANT, "rule-1");

        verify(repository).delete(sampleRule);
        verifyNoInteractions(iptablesExecutor);
    }

    @Test
    void deleteRule_appliedRule_removesFromOsThenDeletes() {
        sampleRule.setRuleStatus(FirewallRule.RuleStatus.APPLIED);
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));
        when(iptablesExecutor.buildCommandString(any(), any())).thenReturn("iptables -D INPUT ...");
        when(iptablesExecutor.execute(any(), eq(IptablesExecutor.Operation.DELETE)))
                .thenReturn(new IptablesExecutor.ExecResult(0, "", ""));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteRule(TENANT, "rule-1");

        verify(iptablesExecutor).execute(sampleRule, IptablesExecutor.Operation.DELETE);
        verify(repository).delete(any());
    }

    // ── Apply ───────────────────────────────────────────────────────────

    @Test
    void applyRuleToOs_success_setsAppliedStatus() {
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));
        when(iptablesExecutor.buildCommandString(any(), any())).thenReturn("iptables -A INPUT ...");
        when(iptablesExecutor.execute(sampleRule, IptablesExecutor.Operation.APPEND))
                .thenReturn(new IptablesExecutor.ExecResult(0, "ok", ""));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FirewallRule result = service.applyRuleToOs(TENANT, "rule-1");

        assertThat(result.getRuleStatus()).isEqualTo(FirewallRule.RuleStatus.APPLIED);
        assertThat(result.getLastError()).isNull();
    }

    @Test
    void applyRuleToOs_failure_setsFailedStatusWithError() {
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));
        when(iptablesExecutor.buildCommandString(any(), any())).thenReturn("iptables -A INPUT ...");
        when(iptablesExecutor.execute(sampleRule, IptablesExecutor.Operation.APPEND))
                .thenReturn(new IptablesExecutor.ExecResult(1, "", "Permission denied"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FirewallRule result = service.applyRuleToOs(TENANT, "rule-1");

        assertThat(result.getRuleStatus()).isEqualTo(FirewallRule.RuleStatus.FAILED);
        assertThat(result.getLastError()).isEqualTo("Permission denied");
    }

    @Test
    void applyRuleToOs_disabledRule_throwsBadRequest() {
        sampleRule.setEnabled(false);
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));

        assertThatThrownBy(() -> service.applyRuleToOs(TENANT, "rule-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void applyRuleToOs_alreadyApplied_throwsConflict() {
        sampleRule.setRuleStatus(FirewallRule.RuleStatus.APPLIED);
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));

        assertThatThrownBy(() -> service.applyRuleToOs(TENANT, "rule-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already applied");
    }

    // ── Remove ──────────────────────────────────────────────────────────

    @Test
    void removeRuleFromOs_success_setsRemovedStatus() {
        sampleRule.setRuleStatus(FirewallRule.RuleStatus.APPLIED);
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));
        when(iptablesExecutor.buildCommandString(any(), any())).thenReturn("iptables -D INPUT ...");
        when(iptablesExecutor.execute(sampleRule, IptablesExecutor.Operation.DELETE))
                .thenReturn(new IptablesExecutor.ExecResult(0, "ok", ""));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FirewallRule result = service.removeRuleFromOs(TENANT, "rule-1");

        assertThat(result.getRuleStatus()).isEqualTo(FirewallRule.RuleStatus.REMOVED);
    }

    @Test
    void removeRuleFromOs_notApplied_throwsConflict() {
        sampleRule.setRuleStatus(FirewallRule.RuleStatus.PENDING);
        when(repository.findByIdAndTenantId("rule-1", TENANT)).thenReturn(Optional.of(sampleRule));

        assertThatThrownBy(() -> service.removeRuleFromOs(TENANT, "rule-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not currently applied");
    }

    // ── Count ───────────────────────────────────────────────────────────

    @Test
    void countRules_delegatesToRepository() {
        when(repository.countByTenantId(TENANT)).thenReturn(5L);
        assertThat(service.countRules(TENANT)).isEqualTo(5);
    }

    @Test
    void countByStatus_delegatesToRepository() {
        when(repository.countByTenantIdAndRuleStatus(TENANT, FirewallRule.RuleStatus.APPLIED)).thenReturn(3L);
        assertThat(service.countByStatus(TENANT, FirewallRule.RuleStatus.APPLIED)).isEqualTo(3);
    }
}
