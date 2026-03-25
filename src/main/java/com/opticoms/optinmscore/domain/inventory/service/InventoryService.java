package com.opticoms.optinmscore.domain.inventory.service;

import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.inventory.model.GNodeB;
import com.opticoms.optinmscore.domain.inventory.model.PduSession;
import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.inventory.repository.GNodeBRepository;
import com.opticoms.optinmscore.domain.inventory.repository.PduSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final GNodeBRepository gNodeBRepository;
    private final ConnectedUeRepository connectedUeRepository;
    private final PduSessionRepository pduSessionRepository;

    // --- gNodeB ---

    public List<GNodeB> getAllGNodeBs(String tenantId) {
        return gNodeBRepository.findByTenantId(tenantId);
    }

    public Page<GNodeB> getAllGNodeBsPaged(String tenantId, Pageable pageable) {
        return gNodeBRepository.findByTenantId(tenantId, pageable);
    }

    public GNodeB getGNodeB(String tenantId, String gnbId) {
        return gNodeBRepository.findByTenantIdAndGnbId(tenantId, gnbId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "gNodeB not found: " + gnbId));
    }

    public long getGNodeBCount(String tenantId) {
        return gNodeBRepository.countByTenantId(tenantId);
    }

    public long getConnectedGNodeBCount(String tenantId) {
        return gNodeBRepository.countByTenantIdAndStatus(tenantId, GNodeB.ConnectionStatus.CONNECTED);
    }

    // --- Connected UEs ---

    public List<ConnectedUe> getAllConnectedUes(String tenantId) {
        return connectedUeRepository.findByTenantId(tenantId);
    }

    public Page<ConnectedUe> getAllConnectedUesPaged(String tenantId, Pageable pageable) {
        return connectedUeRepository.findByTenantId(tenantId, pageable);
    }

    public long getConnectedUeCount(String tenantId) {
        return connectedUeRepository.countByTenantId(tenantId);
    }

    public long getUeCountByStatus(String tenantId, ConnectedUe.UeStatus status) {
        return connectedUeRepository.countByTenantIdAndStatus(tenantId, status);
    }

    // --- PDU Sessions ---

    public List<PduSession> getAllPduSessions(String tenantId) {
        return pduSessionRepository.findByTenantId(tenantId);
    }

    public Page<PduSession> getAllPduSessionsPaged(String tenantId, Pageable pageable) {
        return pduSessionRepository.findByTenantId(tenantId, pageable);
    }

    public long getActiveSessionCount(String tenantId) {
        return pduSessionRepository.countByTenantIdAndStatus(tenantId, PduSession.SessionStatus.ACTIVE);
    }

    public long getSessionCountByDnn(String tenantId, String dnn) {
        return pduSessionRepository.countByTenantIdAndDnn(tenantId, dnn);
    }
}
