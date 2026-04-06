package com.opticoms.optinmscore.domain.networkservice.service;

import com.opticoms.optinmscore.domain.networkservice.model.ServiceInstance;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceStatus;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceType;
import com.opticoms.optinmscore.domain.networkservice.repository.ServiceInstanceRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceInstanceServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String NETWORK_ID = "net-1";

    @Mock private ServiceInstanceRepository repository;

    @InjectMocks
    private ServiceInstanceService service;

    private ServiceInstance instance;

    @BeforeEach
    void setUp() {
        instance = new ServiceInstance();
        instance.setName("CONTROL PLANE");
        instance.setType(ServiceType.CONTROL_PLANE);
        instance.setIpAddresses(List.of("10.20.2.10", "10.20.2.12"));
        instance.setStatus(ServiceStatus.UNKNOWN);
    }

    @Test
    void create_success() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceInstance result = service.create(TENANT, NETWORK_ID, instance);

        assertEquals(TENANT, result.getTenantId());
        assertEquals(NETWORK_ID, result.getNetworkId());
        assertEquals("CONTROL PLANE", result.getName());
        verify(repository).save(any());
    }

    @Test
    void getById_found() {
        when(repository.findByIdAndTenantId("si-1", TENANT)).thenReturn(Optional.of(instance));

        ServiceInstance result = service.getById(TENANT, "si-1");

        assertEquals("CONTROL PLANE", result.getName());
    }

    @Test
    void getById_notFound_throws404() {
        when(repository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById(TENANT, "missing"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void listByNetwork_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ServiceInstance> expected = new PageImpl<>(List.of(instance));
        when(repository.findByTenantIdAndNetworkId(TENANT, NETWORK_ID, pageable)).thenReturn(expected);

        Page<ServiceInstance> result = service.listByNetwork(TENANT, NETWORK_ID, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void update_preservesInternalFields() {
        ServiceInstance existing = new ServiceInstance();
        existing.setId("si-1");
        existing.setVersion(2L);
        existing.setCreatedAt(1000L);
        existing.setCreatedBy("admin");
        existing.setTenantId(TENANT);
        existing.setNetworkId(NETWORK_ID);
        existing.setHealthCheckUrl("http://10.20.2.10:9090");
        existing.setStatus(ServiceStatus.RUNNING);
        existing.setStatusMessage("The service is running.");
        existing.setLastHealthCheck(5000L);

        when(repository.findByIdAndTenantId("si-1", TENANT)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceInstance updated = new ServiceInstance();
        updated.setName("UPDATED CONTROL PLANE");
        updated.setType(ServiceType.CONTROL_PLANE);

        ServiceInstance result = service.update(TENANT, "si-1", updated);

        assertEquals("UPDATED CONTROL PLANE", result.getName());
        assertEquals("si-1", result.getId());
        assertEquals(NETWORK_ID, result.getNetworkId());
        assertEquals("http://10.20.2.10:9090", result.getHealthCheckUrl());
        assertEquals(ServiceStatus.RUNNING, result.getStatus());
        assertEquals("The service is running.", result.getStatusMessage());
        assertEquals(5000L, result.getLastHealthCheck());
    }

    @Test
    void updateStatus_updatesAllThreeFields() {
        instance.setId("si-1");
        when(repository.findById("si-1")).thenReturn(Optional.of(instance));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        long beforeMs = System.currentTimeMillis();
        service.updateStatus("si-1", ServiceStatus.RUNNING, "The service is running.");
        long afterMs = System.currentTimeMillis();

        ArgumentCaptor<ServiceInstance> captor = ArgumentCaptor.forClass(ServiceInstance.class);
        verify(repository).save(captor.capture());

        ServiceInstance saved = captor.getValue();
        assertEquals(ServiceStatus.RUNNING, saved.getStatus());
        assertEquals("The service is running.", saved.getStatusMessage());
        assertNotNull(saved.getLastHealthCheck());
        assertTrue(saved.getLastHealthCheck() >= beforeMs && saved.getLastHealthCheck() <= afterMs);
    }

    @Test
    void updateStatus_nonExistentId_doesNothing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        service.updateStatus("missing", ServiceStatus.ERROR, "unreachable");

        verify(repository, never()).save(any());
    }

    @Test
    void delete_success() {
        when(repository.findByIdAndTenantId("si-1", TENANT)).thenReturn(Optional.of(instance));

        service.delete(TENANT, "si-1");

        verify(repository).delete(instance);
    }

    @Test
    void countByNetwork_delegatesToRepo() {
        when(repository.countByTenantIdAndNetworkId(TENANT, NETWORK_ID)).thenReturn(4L);

        assertEquals(4L, service.countByNetwork(TENANT, NETWORK_ID));
    }
}
