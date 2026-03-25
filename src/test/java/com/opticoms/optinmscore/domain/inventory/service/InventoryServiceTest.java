package com.opticoms.optinmscore.domain.inventory.service;

import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.inventory.model.GNodeB;
import com.opticoms.optinmscore.domain.inventory.model.PduSession;
import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.inventory.repository.GNodeBRepository;
import com.opticoms.optinmscore.domain.inventory.repository.PduSessionRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private GNodeBRepository gNodeBRepository;
    @Mock private ConnectedUeRepository connectedUeRepository;
    @Mock private PduSessionRepository pduSessionRepository;

    @InjectMocks
    private InventoryService service;

    // --- gNodeB tests ---

    @Test
    void getAllGNodeBs_delegatesToRepo() {
        GNodeB gnb = new GNodeB();
        gnb.setGnbId("gnb-001");
        when(gNodeBRepository.findByTenantId(TENANT)).thenReturn(List.of(gnb));

        List<GNodeB> result = service.getAllGNodeBs(TENANT);

        assertEquals(1, result.size());
        assertEquals("gnb-001", result.get(0).getGnbId());
    }

    @Test
    void getAllGNodeBsPaged_delegatesToRepo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<GNodeB> page = new PageImpl<>(List.of(new GNodeB()));
        when(gNodeBRepository.findByTenantId(TENANT, pageable)).thenReturn(page);

        Page<GNodeB> result = service.getAllGNodeBsPaged(TENANT, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getGNodeB_found() {
        GNodeB gnb = new GNodeB();
        gnb.setGnbId("gnb-001");
        when(gNodeBRepository.findByTenantIdAndGnbId(TENANT, "gnb-001")).thenReturn(Optional.of(gnb));

        GNodeB result = service.getGNodeB(TENANT, "gnb-001");

        assertEquals("gnb-001", result.getGnbId());
    }

    @Test
    void getGNodeB_notFound_throws404() {
        when(gNodeBRepository.findByTenantIdAndGnbId(TENANT, "missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getGNodeB(TENANT, "missing"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getGNodeBCount_delegatesToRepo() {
        when(gNodeBRepository.countByTenantId(TENANT)).thenReturn(5L);

        assertEquals(5L, service.getGNodeBCount(TENANT));
    }

    @Test
    void getConnectedGNodeBCount_delegatesToRepo() {
        when(gNodeBRepository.countByTenantIdAndStatus(TENANT, GNodeB.ConnectionStatus.CONNECTED))
                .thenReturn(3L);

        assertEquals(3L, service.getConnectedGNodeBCount(TENANT));
    }

    // --- Connected UE tests ---

    @Test
    void getAllConnectedUes_delegatesToRepo() {
        ConnectedUe ue = new ConnectedUe();
        ue.setImsi("286010000000001");
        when(connectedUeRepository.findByTenantId(TENANT)).thenReturn(List.of(ue));

        List<ConnectedUe> result = service.getAllConnectedUes(TENANT);

        assertEquals(1, result.size());
    }

    @Test
    void getAllConnectedUesPaged_delegatesToRepo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ConnectedUe> page = new PageImpl<>(List.of(new ConnectedUe()));
        when(connectedUeRepository.findByTenantId(TENANT, pageable)).thenReturn(page);

        Page<ConnectedUe> result = service.getAllConnectedUesPaged(TENANT, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getConnectedUeCount_delegatesToRepo() {
        when(connectedUeRepository.countByTenantId(TENANT)).thenReturn(10L);

        assertEquals(10L, service.getConnectedUeCount(TENANT));
    }

    @Test
    void getUeCountByStatus_delegatesToRepo() {
        when(connectedUeRepository.countByTenantIdAndStatus(TENANT, ConnectedUe.UeStatus.CONNECTED))
                .thenReturn(7L);

        assertEquals(7L, service.getUeCountByStatus(TENANT, ConnectedUe.UeStatus.CONNECTED));
    }

    // --- PDU Session tests ---

    @Test
    void getAllPduSessions_delegatesToRepo() {
        PduSession session = new PduSession();
        session.setSessionId("sess-001");
        when(pduSessionRepository.findByTenantId(TENANT)).thenReturn(List.of(session));

        List<PduSession> result = service.getAllPduSessions(TENANT);

        assertEquals(1, result.size());
    }

    @Test
    void getAllPduSessionsPaged_delegatesToRepo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PduSession> page = new PageImpl<>(List.of(new PduSession()));
        when(pduSessionRepository.findByTenantId(TENANT, pageable)).thenReturn(page);

        Page<PduSession> result = service.getAllPduSessionsPaged(TENANT, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getActiveSessionCount_delegatesToRepo() {
        when(pduSessionRepository.countByTenantIdAndStatus(TENANT, PduSession.SessionStatus.ACTIVE))
                .thenReturn(8L);

        assertEquals(8L, service.getActiveSessionCount(TENANT));
    }

    @Test
    void getSessionCountByDnn_delegatesToRepo() {
        when(pduSessionRepository.countByTenantIdAndDnn(TENANT, "internet")).thenReturn(4L);

        assertEquals(4L, service.getSessionCountByDnn(TENANT, "internet"));
    }
}
