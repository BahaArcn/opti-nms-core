package com.opticoms.optinmscore.integration.open5gs;

import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.inventory.model.GNodeB;
import com.opticoms.optinmscore.domain.inventory.model.PduSession;
import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.inventory.repository.GNodeBRepository;
import com.opticoms.optinmscore.domain.inventory.repository.PduSessionRepository;
import com.opticoms.optinmscore.domain.observability.service.AlarmService;
import com.opticoms.optinmscore.domain.performance.service.PmService;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Open5gsSyncSchedulerTest {

    private static final String TENANT_ID = "OPTC-0001/0001/01";
    private static final String AMF_URL = "http://10.244.0.129:9090";
    private static final String SMF_URL = "http://10.244.0.134:9090";

    @Mock private Open5gsClient open5gsClient;
    @Mock private GNodeBRepository gNodeBRepository;
    @Mock private ConnectedUeRepository connectedUeRepository;
    @Mock private PduSessionRepository pduSessionRepository;
    @Mock private AlarmService alarmService;
    @Mock private TenantRepository tenantRepository;
    @Mock private PmService pmService;

    @InjectMocks
    private Open5gsSyncScheduler scheduler;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setTenantId(TENANT_ID);
        testTenant.setName("Test Tenant");
        testTenant.setAmfUrl(AMF_URL);
        testTenant.setSmfUrl(SMF_URL);
        testTenant.setActive(true);

        when(tenantRepository.findByActiveTrue()).thenReturn(List.of(testTenant));
    }

    @Test
    void syncFromOpen5gs_gnbData_savesGnb() {
        Map<String, Object> gnbData = new HashMap<>();
        gnbData.put("gnb_id", "000001");
        gnbData.put("plmn", "28601");
        gnbData.put("num_connected_ues", 5);

        when(open5gsClient.fetchGnbInfo(AMF_URL)).thenReturn(List.of(gnbData));
        when(open5gsClient.fetchUeInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchPduInfo(SMF_URL)).thenReturn(Collections.emptyList());
        when(gNodeBRepository.findByTenantIdAndGnbId(TENANT_ID, "000001")).thenReturn(Optional.empty());
        when(gNodeBRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(connectedUeRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(pduSessionRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        scheduler.syncFromOpen5gs();

        ArgumentCaptor<GNodeB> captor = ArgumentCaptor.forClass(GNodeB.class);
        verify(gNodeBRepository).save(captor.capture());
        GNodeB saved = captor.getValue();
        assertEquals("000001", saved.getGnbId());
        assertEquals(TENANT_ID, saved.getTenantId());
        assertEquals(GNodeB.ConnectionStatus.CONNECTED, saved.getStatus());
        assertEquals(5, saved.getConnectedUeCount());
    }

    @Test
    void syncFromOpen5gs_ueData_savesUe() {
        Map<String, Object> ueData = new HashMap<>();
        ueData.put("supi", "imsi-286010000000001");
        ueData.put("cm_state", "connected");

        when(open5gsClient.fetchGnbInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchUeInfo(AMF_URL)).thenReturn(List.of(ueData));
        when(open5gsClient.fetchPduInfo(SMF_URL)).thenReturn(Collections.emptyList());
        when(gNodeBRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(connectedUeRepository.findByTenantIdAndImsi(TENANT_ID, "286010000000001"))
                .thenReturn(Optional.empty());
        when(connectedUeRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(pduSessionRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        scheduler.syncFromOpen5gs();

        ArgumentCaptor<ConnectedUe> captor = ArgumentCaptor.forClass(ConnectedUe.class);
        verify(connectedUeRepository).save(captor.capture());
        ConnectedUe saved = captor.getValue();
        assertEquals("286010000000001", saved.getImsi());
        assertEquals(ConnectedUe.UeStatus.CONNECTED, saved.getStatus());
    }

    @Test
    void syncFromOpen5gs_ueIdleState() {
        Map<String, Object> ueData = new HashMap<>();
        ueData.put("supi", "imsi-286010000000001");
        ueData.put("cm_state", "idle");

        when(open5gsClient.fetchGnbInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchUeInfo(AMF_URL)).thenReturn(List.of(ueData));
        when(open5gsClient.fetchPduInfo(SMF_URL)).thenReturn(Collections.emptyList());
        when(gNodeBRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(connectedUeRepository.findByTenantIdAndImsi(TENANT_ID, "286010000000001"))
                .thenReturn(Optional.empty());
        when(connectedUeRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(pduSessionRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        scheduler.syncFromOpen5gs();

        ArgumentCaptor<ConnectedUe> captor = ArgumentCaptor.forClass(ConnectedUe.class);
        verify(connectedUeRepository).save(captor.capture());
        assertEquals(ConnectedUe.UeStatus.IDLE, captor.getValue().getStatus());
    }

    @Test
    void syncFromOpen5gs_pduData_savesSession() {
        Map<String, Object> snssai = new HashMap<>();
        snssai.put("sst", 1);
        snssai.put("sd", "FFFFFF");

        Map<String, Object> pduData = new HashMap<>();
        pduData.put("psi", 1);
        pduData.put("dnn", "internet");
        pduData.put("ipv4", "10.45.0.2");
        pduData.put("pdu_state", "active");
        pduData.put("snssai", snssai);

        Map<String, Object> ueEntry = new HashMap<>();
        ueEntry.put("supi", "imsi-286010000000001");
        ueEntry.put("pdu", List.of(pduData));

        when(open5gsClient.fetchGnbInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchUeInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchPduInfo(SMF_URL)).thenReturn(List.of(ueEntry));
        when(gNodeBRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(connectedUeRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(pduSessionRepository.findByTenantIdAndSessionId(eq(TENANT_ID), anyString()))
                .thenReturn(Optional.empty());
        when(pduSessionRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        scheduler.syncFromOpen5gs();

        ArgumentCaptor<PduSession> captor = ArgumentCaptor.forClass(PduSession.class);
        verify(pduSessionRepository).save(captor.capture());
        PduSession saved = captor.getValue();
        assertEquals("internet", saved.getDnn());
        assertEquals("10.45.0.2", saved.getUeIpAddress());
        assertEquals(PduSession.SessionStatus.ACTIVE, saved.getStatus());
        assertEquals(1, saved.getSst());
    }

    @Test
    void syncFromOpen5gs_staleGnb_markedDisconnected() {
        GNodeB staleGnb = new GNodeB();
        staleGnb.setGnbId("old-gnb");
        staleGnb.setTenantId(TENANT_ID);
        staleGnb.setStatus(GNodeB.ConnectionStatus.CONNECTED);

        when(open5gsClient.fetchGnbInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchUeInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchPduInfo(SMF_URL)).thenReturn(Collections.emptyList());
        when(gNodeBRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(staleGnb));
        when(connectedUeRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(pduSessionRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        scheduler.syncFromOpen5gs();

        ArgumentCaptor<GNodeB> captor = ArgumentCaptor.forClass(GNodeB.class);
        verify(gNodeBRepository).save(captor.capture());
        assertEquals(GNodeB.ConnectionStatus.DISCONNECTED, captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getConnectedUeCount());
    }

    @Test
    void syncFromOpen5gs_stalePduSession_markedReleased() {
        PduSession stale = new PduSession();
        stale.setSessionId("old-session");
        stale.setTenantId(TENANT_ID);
        stale.setStatus(PduSession.SessionStatus.ACTIVE);

        when(open5gsClient.fetchGnbInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchUeInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchPduInfo(SMF_URL)).thenReturn(Collections.emptyList());
        when(gNodeBRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(connectedUeRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(pduSessionRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(stale));

        scheduler.syncFromOpen5gs();

        ArgumentCaptor<PduSession> captor = ArgumentCaptor.forClass(PduSession.class);
        verify(pduSessionRepository).save(captor.capture());
        assertEquals(PduSession.SessionStatus.RELEASED, captor.getValue().getStatus());
    }

    @Test
    void syncFromOpen5gs_exceptionDuringSync_raisesAlarm() {
        when(open5gsClient.fetchGnbInfo(AMF_URL)).thenThrow(new RuntimeException("Network error"));

        scheduler.syncFromOpen5gs();

        verify(alarmService).raiseAlarm(eq(TENANT_ID), argThat(alarm ->
                "CONNECTIVITY_FAILURE".equals(alarm.getAlarmType())));
    }

    @Test
    void syncFromOpen5gs_emptyGnbList_marksAllDisconnected() {
        GNodeB connected = new GNodeB();
        connected.setGnbId("gnb-1");
        connected.setStatus(GNodeB.ConnectionStatus.CONNECTED);

        when(open5gsClient.fetchGnbInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchUeInfo(AMF_URL)).thenReturn(Collections.emptyList());
        when(open5gsClient.fetchPduInfo(SMF_URL)).thenReturn(Collections.emptyList());
        when(gNodeBRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(connected));
        when(connectedUeRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(pduSessionRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        scheduler.syncFromOpen5gs();

        verify(gNodeBRepository).save(argThat(g ->
                g.getStatus() == GNodeB.ConnectionStatus.DISCONNECTED));
    }

    @Test
    void syncFromOpen5gs_noActiveTenants_skipsSync() {
        when(tenantRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        scheduler.syncFromOpen5gs();

        verifyNoInteractions(open5gsClient);
    }
}
