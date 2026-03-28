package com.opticoms.optinmscore.domain.policy.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.policy.model.Policy;
import com.opticoms.optinmscore.domain.policy.service.PolicyService;
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

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policy Management")
public class PolicyController {

    private final PolicyService policyService;

    @Operation(summary = "Create a new policy")
    @PostMapping
    public ResponseEntity<Policy> createPolicy(
            HttpServletRequest request,
            @Valid @RequestBody Policy policy) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(policyService.createPolicy(tenantId, policy));
    }

    @Operation(summary = "List all policies with pagination")
    @GetMapping
    public ResponseEntity<Page<Policy>> listPolicies(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(policyService.listPolicies(tenantId, pageable));
    }

    @Operation(summary = "Get policy by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Policy> getPolicy(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(policyService.getPolicy(tenantId, id));
    }

    @Operation(summary = "Update policy by ID")
    @PutMapping("/{id}")
    public ResponseEntity<Policy> updatePolicy(
            HttpServletRequest request,
            @PathVariable String id,
            @Valid @RequestBody Policy policy) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(policyService.updatePolicy(tenantId, id, policy));
    }

    @Operation(summary = "Delete policy by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(
            HttpServletRequest request,
            @PathVariable String id) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        policyService.deletePolicy(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get policy count")
    @GetMapping("/count")
    public ResponseEntity<Long> countPolicies(HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(policyService.countPolicies(tenantId));
    }
}
