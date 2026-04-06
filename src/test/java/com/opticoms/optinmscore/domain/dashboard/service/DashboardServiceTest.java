package com.opticoms.optinmscore.domain.dashboard.service;

import com.opticoms.optinmscore.domain.edgelocation.repository.EdgeLocationRepository;
import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import com.opticoms.optinmscore.domain.inventory.model.GNodeB;
import com.opticoms.optinmscore.domain.inventory.model.PduSession;
import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.inventory.repository.GNodeBRepository;
import com.opticoms.optinmscore.domain.inventory.repository.PduSessionRepository;
import com.opticoms.optinmscore.domain.observability.model.Alarm;
import com.opticoms.optinmscore.domain.observability.repository.AlarmRepository;
import com.opticoms.optinmscore.domain.subscriber.repository.SubscriberRepository;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
import com.opticoms.optinmscore.integration.open5gs.Open5gsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final String TENANT_ID = "OPTC-0001/0001/01";
    private static final String AMF_URL = "http://10.244.0.129:9090";
    private static final String SMF_URL = "http://10.244.0.134:9090";

    @Mock private SubscriberRepository subscriberRepository;
    @Mock private GNodeBRepository gNodeBRepository;
    @Mock private ConnectedUeRepository connectedUeRepository;
    @Mock private PduSessionRepository pduSessionRepository;
    @Mock private AlarmRepository alarmRepository;
    @Mock private Open5gsClient open5gsClient;
    @Mock private TenantService tenantService;
    @Mock private EdgeLocationRepository edgeLocationRepository;
    @Mock private LicenseService licenseService;

    @InjectMocks
    private DashboardService service;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setTenantId(TENANT_ID);
        testTenant.setName("Test Tenant");
        testTenant.setAmfUrl(AMF_URL);
        testTenant.setSmfUrl(SMF_URL);
        testTenant.setActive(true);

        when(tenantService.getTenant(TENANT_ID)).thenReturn(testTenant);
    }

    @Test
    void getDashboardSummary_healthy() {
        stubRepositories(100, 5, 3, 2, 10, 3, 15, 8, 0, 0, 0);
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(true);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);

        DashboardService.DashboardSummary summary = service.getDashboardSummary(TENANT_ID);

        assertEquals(100, summary.getTotalSubscribers());
        assertEquals(5, summary.getTotalGNodeBs());
        assertEquals(3, summary.getConnectedGNodeBs());
        assertEquals(2, summary.getDisconnectedGNodeBs());
        assertEquals(10, summary.getConnectedUes());
        assertEquals(3, summary.getIdleUes());
        assertEquals(15, summary.getTotalUes());
        assertEquals(8, summary.getActiveSessions());
        assertEquals(0, summary.getActiveAlarms());
        assertEquals(DashboardService.SystemStatus.HEALTHY, summary.getSystemStatus());
        assertTrue(summary.isAmfReachable());
        assertTrue(summary.isSmfReachable());
        verify(alarmRepository, times(1)).countByTenantIdAndSeverityAndStatus(
                TENANT_ID, Alarm.Severity.CRITICAL, Alarm.AlarmStatus.ACTIVE);
        verify(alarmRepository, times(1)).countByTenantIdAndSeverityAndStatus(
                TENANT_ID, Alarm.Severity.MAJOR, Alarm.AlarmStatus.ACTIVE);
    }

    @Test
    void systemStatus_criticalAlarms_returnsDown() {
        stubRepositories(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0);
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(true);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);

        DashboardService.DashboardSummary summary = service.getDashboardSummary(TENANT_ID);

        assertEquals(DashboardService.SystemStatus.DOWN, summary.getSystemStatus());
    }

    @Test
    void systemStatus_bothNfsDown_returnsDown() {
        stubRepositories(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(false);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(false);

        DashboardService.DashboardSummary summary = service.getDashboardSummary(TENANT_ID);

        assertEquals(DashboardService.SystemStatus.DOWN, summary.getSystemStatus());
    }

    @Test
    void systemStatus_majorAlarms_returnsDegraded() {
        stubRepositories(0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2);
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(true);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);

        DashboardService.DashboardSummary summary = service.getDashboardSummary(TENANT_ID);

        assertEquals(DashboardService.SystemStatus.DEGRADED, summary.getSystemStatus());
    }

    @Test
    void systemStatus_amfDown_returnsDegraded() {
        stubRepositories(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(false);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);

        DashboardService.DashboardSummary summary = service.getDashboardSummary(TENANT_ID);

        assertEquals(DashboardService.SystemStatus.DEGRADED, summary.getSystemStatus());
    }

    @Test
    void systemStatus_smfDown_returnsDegraded() {
        stubRepositories(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(true);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(false);

        DashboardService.DashboardSummary summary = service.getDashboardSummary(TENANT_ID);

        assertEquals(DashboardService.SystemStatus.DEGRADED, summary.getSystemStatus());
    }

    private void stubRepositories(long subscribers, long gnbs, long connGnbs, long discGnbs,
                                  long connUes, long idleUes, long totalUes,
                                  long activeSessions, long activeAlarms,
                                  long critAlarms, long majorAlarms) {
        when(subscriberRepository.countByTenantId(TENANT_ID)).thenReturn(subscribers);
        when(gNodeBRepository.countByTenantId(TENANT_ID)).thenReturn(gnbs);
        when(gNodeBRepository.countByTenantIdAndStatus(TENANT_ID, GNodeB.ConnectionStatus.CONNECTED)).thenReturn(connGnbs);
        when(gNodeBRepository.countByTenantIdAndStatus(TENANT_ID, GNodeB.ConnectionStatus.DISCONNECTED)).thenReturn(discGnbs);
        when(connectedUeRepository.countByTenantIdAndStatus(TENANT_ID, ConnectedUe.UeStatus.CONNECTED)).thenReturn(connUes);
        when(connectedUeRepository.countByTenantIdAndStatus(TENANT_ID, ConnectedUe.UeStatus.IDLE)).thenReturn(idleUes);
        when(connectedUeRepository.countByTenantId(TENANT_ID)).thenReturn(totalUes);
        when(pduSessionRepository.countByTenantIdAndStatus(TENANT_ID, PduSession.SessionStatus.ACTIVE)).thenReturn(activeSessions);
        when(alarmRepository.countByTenantIdAndStatus(TENANT_ID, Alarm.AlarmStatus.ACTIVE)).thenReturn(activeAlarms);
        when(alarmRepository.countByTenantIdAndSeverityAndStatus(TENANT_ID, Alarm.Severity.CRITICAL, Alarm.AlarmStatus.ACTIVE)).thenReturn(critAlarms);
        when(alarmRepository.countByTenantIdAndSeverityAndStatus(TENANT_ID, Alarm.Severity.MAJOR, Alarm.AlarmStatus.ACTIVE)).thenReturn(majorAlarms);
    }
}
