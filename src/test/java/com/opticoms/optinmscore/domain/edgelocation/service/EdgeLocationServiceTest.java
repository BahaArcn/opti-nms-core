package com.opticoms.optinmscore.domain.edgelocation.service;

import com.opticoms.optinmscore.domain.edgelocation.model.EdgeLocation;
import com.opticoms.optinmscore.domain.edgelocation.repository.EdgeLocationRepository;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
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
class EdgeLocationServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private EdgeLocationRepository repository;
    @Mock private LicenseService licenseService;

    @InjectMocks
    private EdgeLocationService service;

    private EdgeLocation edgeLocation;

    @BeforeEach
    void setUp() {
        edgeLocation = new EdgeLocation();
        edgeLocation.setName("Istanbul-DC-1");
        edgeLocation.setDescription("Primary DC");
        edgeLocation.setStatus(EdgeLocation.EdgeLocationStatus.ACTIVE);
    }

    @Test
    void create_success() {
        when(repository.existsByTenantIdAndName(TENANT, "Istanbul-DC-1")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EdgeLocation result = service.create(TENANT, edgeLocation);

        assertEquals(TENANT, result.getTenantId());
        assertEquals("Istanbul-DC-1", result.getName());
        verify(repository).save(any());
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(repository.existsByTenantIdAndName(TENANT, "Istanbul-DC-1")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(TENANT, edgeLocation));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void list_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<EdgeLocation> expected = new PageImpl<>(List.of(edgeLocation));
        when(repository.findByTenantId(TENANT, pageable)).thenReturn(expected);

        Page<EdgeLocation> result = service.list(TENANT, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getById_found() {
        when(repository.findByIdAndTenantId("id-1", TENANT)).thenReturn(Optional.of(edgeLocation));

        EdgeLocation result = service.getById(TENANT, "id-1");

        assertEquals("Istanbul-DC-1", result.getName());
    }

    @Test
    void getById_notFound_throws404() {
        when(repository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById(TENANT, "missing"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void update_success() {
        EdgeLocation existing = new EdgeLocation();
        existing.setId("id-1");
        existing.setName("Istanbul-DC-1");
        existing.setVersion(1L);

        when(repository.findByIdAndTenantId("id-1", TENANT)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EdgeLocation updated = new EdgeLocation();
        updated.setName("Istanbul-DC-2");

        EdgeLocation result = service.update(TENANT, "id-1", updated);

        assertEquals("Istanbul-DC-2", result.getName());
        assertEquals("id-1", result.getId());
    }

    @Test
    void delete_success() {
        when(repository.findByIdAndTenantId("id-1", TENANT)).thenReturn(Optional.of(edgeLocation));

        service.delete(TENANT, "id-1");

        verify(repository).delete(edgeLocation);
    }

    @Test
    void count_delegatesToRepo() {
        when(repository.countByTenantId(TENANT)).thenReturn(5L);

        assertEquals(5L, service.count(TENANT));
    }
}
