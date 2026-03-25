package com.opticoms.optinmscore.domain.firewall.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import com.opticoms.optinmscore.domain.firewall.service.FirewallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/firewall/rules")
@RequiredArgsConstructor
@Tag(name = "Firewall Management", description = "iptables-based OS-level firewall rule management")
public class FirewallController {

    private final FirewallService firewallService;

    @Operation(summary = "Create a new firewall rule (ADMIN only)")
    @PostMapping
    public ResponseEntity<FirewallRule> createRule(
            HttpServletRequest request,
            @Valid @RequestBody FirewallRule rule) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(firewallService.createRule(tenantId, rule));
    }

    @Operation(summary = "List firewall rules with pagination (optional enabled filter)")
    @GetMapping
    public ResponseEntity<?> listRules(
            HttpServletRequest request,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "priority") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        if (Boolean.TRUE.equals(enabled)) {
            return ResponseEntity.ok(firewallService.listEnabledRules(tenantId));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(firewallService.listRules(tenantId, pageable));
    }

    @Operation(summary = "Get firewall rule by ID")
    @GetMapping("/{ruleId}")
    public ResponseEntity<FirewallRule> getRule(
            HttpServletRequest request,
            @PathVariable String ruleId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallService.getRuleById(tenantId, ruleId));
    }

    @Operation(summary = "Filter rules by chain (INPUT, OUTPUT, FORWARD)")
    @GetMapping("/chain/{chain}")
    public ResponseEntity<List<FirewallRule>> listByChain(
            HttpServletRequest request,
            @PathVariable FirewallRule.Chain chain) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallService.listRulesByChain(tenantId, chain));
    }

    @Operation(summary = "Filter rules by status (PENDING, APPLIED, FAILED, REMOVED)")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FirewallRule>> listByStatus(
            HttpServletRequest request,
            @PathVariable FirewallRule.RuleStatus status) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallService.listRulesByStatus(tenantId, status));
    }

    @Operation(summary = "Update a firewall rule (must not be APPLIED)")
    @PutMapping("/{ruleId}")
    public ResponseEntity<FirewallRule> updateRule(
            HttpServletRequest request,
            @PathVariable String ruleId,
            @Valid @RequestBody FirewallRule rule) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallService.updateRule(tenantId, ruleId, rule));
    }

    @Operation(summary = "Delete a firewall rule (auto-removes from OS if applied)")
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(
            HttpServletRequest request,
            @PathVariable String ruleId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        firewallService.deleteRule(tenantId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Apply rule to OS (execute iptables -A)")
    @PostMapping("/{ruleId}/apply")
    public ResponseEntity<FirewallRule> applyRule(
            HttpServletRequest request,
            @PathVariable String ruleId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallService.applyRuleToOs(tenantId, ruleId));
    }

    @Operation(summary = "Remove rule from OS (execute iptables -D)")
    @PostMapping("/{ruleId}/remove")
    public ResponseEntity<FirewallRule> removeRule(
            HttpServletRequest request,
            @PathVariable String ruleId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallService.removeRuleFromOs(tenantId, ruleId));
    }

    @Operation(summary = "Get total rule count")
    @GetMapping("/count")
    public ResponseEntity<Long> countRules(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallService.countRules(tenantId));
    }
}
