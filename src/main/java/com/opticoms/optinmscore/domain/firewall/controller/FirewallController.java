package com.opticoms.optinmscore.domain.firewall.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.firewall.dto.FirewallRuleRequest;
import com.opticoms.optinmscore.domain.firewall.dto.FirewallRuleResponse;
import com.opticoms.optinmscore.domain.firewall.mapper.FirewallRuleMapper;
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
    private final FirewallRuleMapper firewallRuleMapper;

    @Operation(summary = "Create a new firewall rule (ADMIN only)")
    @PostMapping
    public ResponseEntity<FirewallRuleResponse> createRule(
            HttpServletRequest request,
            @Valid @RequestBody FirewallRuleRequest ruleRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(firewallRuleMapper.toResponse(
                        firewallService.createRule(tenantId, firewallRuleMapper.toEntity(ruleRequest))));
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
            return ResponseEntity.ok(firewallRuleMapper.toResponseList(
                    firewallService.listEnabledRules(tenantId)));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(firewallService.listRules(tenantId, pageable)
                .map(firewallRuleMapper::toResponse));
    }

    @Operation(summary = "Get firewall rule by ID")
    @GetMapping("/{ruleId}")
    public ResponseEntity<FirewallRuleResponse> getRule(
            HttpServletRequest request,
            @PathVariable String ruleId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallRuleMapper.toResponse(
                firewallService.getRuleById(tenantId, ruleId)));
    }

    @Operation(summary = "Filter rules by chain (INPUT, OUTPUT, FORWARD)")
    @GetMapping("/chain/{chain}")
    public ResponseEntity<List<FirewallRuleResponse>> listByChain(
            HttpServletRequest request,
            @PathVariable FirewallRule.Chain chain) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallRuleMapper.toResponseList(
                firewallService.listRulesByChain(tenantId, chain)));
    }

    @Operation(summary = "Filter rules by status (PENDING, APPLIED, FAILED, REMOVED)")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FirewallRuleResponse>> listByStatus(
            HttpServletRequest request,
            @PathVariable FirewallRule.RuleStatus status) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallRuleMapper.toResponseList(
                firewallService.listRulesByStatus(tenantId, status)));
    }

    @Operation(summary = "Update a firewall rule (must not be APPLIED)")
    @PutMapping("/{ruleId}")
    public ResponseEntity<FirewallRuleResponse> updateRule(
            HttpServletRequest request,
            @PathVariable String ruleId,
            @Valid @RequestBody FirewallRuleRequest ruleRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallRuleMapper.toResponse(
                firewallService.updateRule(tenantId, ruleId, firewallRuleMapper.toEntity(ruleRequest))));
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
    public ResponseEntity<FirewallRuleResponse> applyRule(
            HttpServletRequest request,
            @PathVariable String ruleId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallRuleMapper.toResponse(
                firewallService.applyRuleToOs(tenantId, ruleId)));
    }

    @Operation(summary = "Remove rule from OS (execute iptables -D)")
    @PostMapping("/{ruleId}/remove")
    public ResponseEntity<FirewallRuleResponse> removeRule(
            HttpServletRequest request,
            @PathVariable String ruleId) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallRuleMapper.toResponse(
                firewallService.removeRuleFromOs(tenantId, ruleId)));
    }

    @Operation(summary = "Get total rule count")
    @GetMapping("/count")
    public ResponseEntity<Long> countRules(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(firewallService.countRules(tenantId));
    }
}
