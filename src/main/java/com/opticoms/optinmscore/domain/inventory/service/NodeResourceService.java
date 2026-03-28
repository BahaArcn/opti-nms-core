package com.opticoms.optinmscore.domain.inventory.service;

import com.opticoms.optinmscore.domain.inventory.model.NodeResource;
import com.opticoms.optinmscore.domain.inventory.repository.NodeResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NodeResourceService {

    private static final double CRITICAL_THRESHOLD = 90.0;
    private static final double WARNING_THRESHOLD = 75.0;

    private final NodeResourceRepository repository;

    /**
     * Upsert: if a node with the same tenantId+nodeId exists, update it; otherwise create.
     * Status is auto-calculated from resource percentages.
     */
    public NodeResource reportNodeResource(String tenantId, NodeResource report) {
        Optional<NodeResource> existing = repository.findByTenantIdAndNodeId(tenantId, report.getNodeId());

        NodeResource node;
        if (existing.isPresent()) {
            node = existing.get();
            node.setNodeName(report.getNodeName());
            node.setEdgeLocationId(report.getEdgeLocationId());
            node.setCpuPercent(report.getCpuPercent());
            node.setMemoryPercent(report.getMemoryPercent());
            node.setDiskPercent(report.getDiskPercent());
        } else {
            node = report;
            node.setTenantId(tenantId);
        }

        node.setLastReportedAt(System.currentTimeMillis());
        node.setStatus(calculateStatus(node));

        log.debug("Node resource report: nodeId={}, cpu={}%, mem={}%, disk={}%, status={}",
                node.getNodeId(), node.getCpuPercent(), node.getMemoryPercent(),
                node.getDiskPercent(), node.getStatus());

        return repository.save(node);
    }

    public Page<NodeResource> getNodeResources(String tenantId, Pageable pageable) {
        return repository.findByTenantId(tenantId, pageable);
    }

    public NodeResource getNodeResource(String tenantId, String nodeId) {
        return repository.findByTenantIdAndNodeId(tenantId, nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Node resource not found: " + nodeId));
    }

    private NodeResource.NodeStatus calculateStatus(NodeResource node) {
        double cpu = node.getCpuPercent() != null ? node.getCpuPercent() : 0;
        double mem = node.getMemoryPercent() != null ? node.getMemoryPercent() : 0;
        double disk = node.getDiskPercent() != null ? node.getDiskPercent() : 0;

        if (cpu >= CRITICAL_THRESHOLD || mem >= CRITICAL_THRESHOLD || disk >= CRITICAL_THRESHOLD) {
            return NodeResource.NodeStatus.CRITICAL;
        } else if (cpu >= WARNING_THRESHOLD || mem >= WARNING_THRESHOLD || disk >= WARNING_THRESHOLD) {
            return NodeResource.NodeStatus.WARNING;
        }
        return NodeResource.NodeStatus.HEALTHY;
    }
}
