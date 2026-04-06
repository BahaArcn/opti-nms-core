package com.opticoms.optinmscore.domain.policy.service;

import com.opticoms.optinmscore.domain.policy.model.Policy;
import com.opticoms.optinmscore.domain.policy.repository.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private PolicyRepository policyRepository;
    @InjectMocks private PolicyService policyService;

    @Test
    void createPolicy_savesWithTenantId() {
        Policy policy = new Policy();
        policy.setName("default");
        when(policyRepository.existsByTenantIdAndName(TENANT, "default")).thenReturn(false);
        when(policyRepository.save(any())).thenAnswer(inv -> {
            Policy p = inv.getArgument(0);
            p.setId("pol-1");
            return p;
        });

        Policy result = policyService.createPolicy(TENANT, policy);

        assertEquals(TENANT, result.getTenantId());
        assertEquals("pol-1", result.getId());
    }

    @Test
    void createPolicy_duplicateName_throwsConflict() {
        Policy policy = new Policy();
        policy.setName("default");
        when(policyRepository.existsByTenantIdAndName(TENANT, "default")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> policyService.createPolicy(TENANT, policy));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void getPolicy_notFound_throws404() {
        when(policyRepository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> policyService.getPolicy(TENANT, "missing"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void listPolicies_returnsPaged() {
        Policy p = new Policy();
        p.setId("p1");
        when(policyRepository.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(p)));

        Page<Policy> result = policyService.listPolicies(TENANT, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void deletePolicy_callsRepository() {
        Policy policy = new Policy();
        policy.setId("p1");
        when(policyRepository.findByIdAndTenantId("p1", TENANT)).thenReturn(Optional.of(policy));

        policyService.deletePolicy(TENANT, "p1");

        verify(policyRepository).delete(policy);
    }

    @Test
    void existsForTenant_returnsCorrectBoolean() {
        when(policyRepository.existsByIdAndTenantId("p1", TENANT)).thenReturn(true);
        assertTrue(policyService.existsForTenant(TENANT, "p1"));

        when(policyRepository.existsByIdAndTenantId("p2", TENANT)).thenReturn(false);
        assertFalse(policyService.existsForTenant(TENANT, "p2"));
    }
}
