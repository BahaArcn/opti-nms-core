package com.opticoms.optinmscore.domain.inventory.service;

import com.opticoms.optinmscore.domain.inventory.model.NodeResource;
import com.opticoms.optinmscore.domain.inventory.repository.NodeResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeResourceServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private NodeResourceRepository repository;

    @InjectMocks
    private NodeResourceService service;

    private NodeResource report;

    @BeforeEach
    void setUp() {
        report = new NodeResource();
        report.setNodeId("node-amf-01");
        report.setNodeName("AMF Server 1");
        report.setCpuPercent(45.0);
        report.setMemoryPercent(62.0);
        report.setDiskPercent(38.0);
    }

    @Test
    void reportNodeResource_newNode_createsWithHealthyStatus() {
        when(repository.findByTenantIdAndNodeId(TENANT, "node-amf-01")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NodeResource result = service.reportNodeResource(TENANT, report);

        assertEquals(TENANT, result.getTenantId());
        assertEquals(NodeResource.NodeStatus.HEALTHY, result.getStatus());
        assertNotNull(result.getLastReportedAt());
    }

    @Test
    void reportNodeResource_existingNode_updatesInPlace() {
        NodeResource existing = new NodeResource();
        existing.setId("existing-id");
        existing.setNodeId("node-amf-01");
        existing.setTenantId(TENANT);

        when(repository.findByTenantIdAndNodeId(TENANT, "node-amf-01")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NodeResource result = service.reportNodeResource(TENANT, report);

        assertEquals("existing-id", result.getId());
        assertEquals(45.0, result.getCpuPercent());
    }

    @Test
    void reportNodeResource_highCpu_returnsCritical() {
        report.setCpuPercent(95.0);
        when(repository.findByTenantIdAndNodeId(TENANT, "node-amf-01")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NodeResource result = service.reportNodeResource(TENANT, report);

        assertEquals(NodeResource.NodeStatus.CRITICAL, result.getStatus());
    }

    @Test
    void reportNodeResource_highMemory_returnsWarning() {
        report.setMemoryPercent(80.0);
        when(repository.findByTenantIdAndNodeId(TENANT, "node-amf-01")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NodeResource result = service.reportNodeResource(TENANT, report);

        assertEquals(NodeResource.NodeStatus.WARNING, result.getStatus());
    }

    @Test
    void getNodeResource_found() {
        when(repository.findByTenantIdAndNodeId(TENANT, "node-amf-01")).thenReturn(Optional.of(report));

        NodeResource result = service.getNodeResource(TENANT, "node-amf-01");

        assertEquals("node-amf-01", result.getNodeId());
    }

    @Test
    void getNodeResource_notFound_throws404() {
        when(repository.findByTenantIdAndNodeId(TENANT, "missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getNodeResource(TENANT, "missing"));
        assertEquals(404, ex.getStatusCode().value());
    }
}
