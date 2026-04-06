package com.opticoms.optinmscore.domain.networkservice.service;

import com.opticoms.optinmscore.domain.networkservice.dto.NetworkSummaryResponse;
import com.opticoms.optinmscore.domain.networkservice.model.Network;
import com.opticoms.optinmscore.domain.networkservice.model.NetworkStatus;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceStatus;
import com.opticoms.optinmscore.domain.networkservice.repository.NetworkRepository;
import com.opticoms.optinmscore.domain.networkservice.repository.ServiceInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class NetworkGroupServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private NetworkRepository networkRepository;
    @Mock private ServiceInstanceRepository serviceInstanceRepository;

    @InjectMocks
    private NetworkGroupService service;

    private Network network;

    @BeforeEach
    void setUp() {
        network = new Network();
        network.setName("Default Network");
        network.setDescription("Primary production network");
        network.setStatus(NetworkStatus.ACTIVE);
    }

    @Test
    void create_success() {
        when(networkRepository.existsByTenantIdAndName(TENANT, "Default Network")).thenReturn(false);
        when(networkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Network result = service.create(TENANT, network);

        assertEquals(TENANT, result.getTenantId());
        assertEquals("Default Network", result.getName());
        verify(networkRepository).save(any());
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(networkRepository.existsByTenantIdAndName(TENANT, "Default Network")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(TENANT, network));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void getById_found() {
        when(networkRepository.findByIdAndTenantId("id-1", TENANT)).thenReturn(Optional.of(network));

        Network result = service.getById(TENANT, "id-1");

        assertEquals("Default Network", result.getName());
    }

    @Test
    void getById_notFound_throws404() {
        when(networkRepository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById(TENANT, "missing"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void list_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Network> expected = new PageImpl<>(List.of(network));
        when(networkRepository.findByTenantId(TENANT, pageable)).thenReturn(expected);

        Page<Network> result = service.list(TENANT, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void update_success() {
        Network existing = new Network();
        existing.setId("id-1");
        existing.setName("Default Network");
        existing.setVersion(1L);

        when(networkRepository.findByIdAndTenantId("id-1", TENANT)).thenReturn(Optional.of(existing));
        when(networkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Network updated = new Network();
        updated.setName("Production Network");

        Network result = service.update(TENANT, "id-1", updated);

        assertEquals("Production Network", result.getName());
        assertEquals("id-1", result.getId());
    }

    @Test
    void delete_cascadesToServiceInstances() {
        network.setId("net-1");
        when(networkRepository.findByIdAndTenantId("net-1", TENANT)).thenReturn(Optional.of(network));

        service.delete(TENANT, "net-1");

        verify(serviceInstanceRepository).deleteByTenantIdAndNetworkId(TENANT, "net-1");
        verify(networkRepository).delete(network);
    }

    @Test
    void count_delegatesToRepo() {
        when(networkRepository.countByTenantId(TENANT)).thenReturn(3L);

        assertEquals(3L, service.count(TENANT));
    }

    @Test
    void getSummary_returnsCorrectCounts() {
        network.setId("net-1");
        when(networkRepository.findByIdAndTenantId("net-1", TENANT)).thenReturn(Optional.of(network));
        when(serviceInstanceRepository.countByTenantIdAndNetworkId(TENANT, "net-1")).thenReturn(5L);
        when(serviceInstanceRepository.countByTenantIdAndNetworkIdAndStatus(TENANT, "net-1", ServiceStatus.RUNNING)).thenReturn(3L);
        when(serviceInstanceRepository.countByTenantIdAndNetworkIdAndStatus(TENANT, "net-1", ServiceStatus.STOPPED)).thenReturn(1L);
        when(serviceInstanceRepository.countByTenantIdAndNetworkIdAndStatus(TENANT, "net-1", ServiceStatus.ERROR)).thenReturn(1L);

        NetworkSummaryResponse summary = service.getSummary(TENANT, "net-1");

        assertEquals("net-1", summary.getNetworkId());
        assertEquals("Default Network", summary.getNetworkName());
        assertEquals(5L, summary.getTotalServices());
        assertEquals(3L, summary.getRunningServices());
        assertEquals(1L, summary.getStoppedServices());
        assertEquals(1L, summary.getErrorServices());
    }
}
