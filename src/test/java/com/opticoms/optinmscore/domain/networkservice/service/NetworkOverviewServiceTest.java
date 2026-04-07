package com.opticoms.optinmscore.domain.networkservice.service;

import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse.ServiceStatusItem;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
import com.opticoms.optinmscore.integration.open5gs.Open5gsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkOverviewServiceTest {

    private static final String TENANT_ID = "OPTC-0001/0001/01";
    private static final String AMF_URL = "http://10.244.0.129:9090";
    private static final String SMF_URL = "http://10.244.0.134:9090";
    private static final String UPF_URL = "http://10.244.0.140:9090/metrics";

    @Mock private TenantService tenantService;
    @Mock private Open5gsClient open5gsClient;

    @InjectMocks
    private NetworkOverviewService service;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setTenantId(TENANT_ID);
        testTenant.setAmfUrl(AMF_URL);
        testTenant.setSmfUrl(SMF_URL);
        testTenant.setUpfMetricsUrl(UPF_URL);
        when(tenantService.getTenant(TENANT_ID)).thenReturn(testTenant);
    }

    @Test
    void allHealthy_returnsThreeRunning() {
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(true);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);
        when(open5gsClient.isUpfHealthy(UPF_URL)).thenReturn(true);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        List<ServiceStatusItem> services = response.getServices();
        assertEquals(3, services.size());
        assertAll(
                () -> assertEquals("RUNNING", services.get(0).getStatus()),
                () -> assertEquals("CONTROL_PLANE", services.get(0).getType()),
                () -> assertEquals("The service is running.", services.get(0).getStatusMessage()),
                () -> assertEquals(List.of("10.244.0.129"), services.get(0).getIpAddresses()),
                () -> assertEquals("RUNNING", services.get(1).getStatus()),
                () -> assertEquals("POLICY_SUBSCRIBER_MGMT", services.get(1).getType()),
                () -> assertEquals(List.of("10.244.0.134"), services.get(1).getIpAddresses()),
                () -> assertEquals("RUNNING", services.get(2).getStatus()),
                () -> assertEquals("DATA_PLANE", services.get(2).getType()),
                () -> assertEquals(List.of("10.244.0.140"), services.get(2).getIpAddresses())
        );
    }

    @Test
    void amfUnreachable_controlPlaneError() {
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(false);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);
        when(open5gsClient.isUpfHealthy(UPF_URL)).thenReturn(true);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        List<ServiceStatusItem> services = response.getServices();
        assertEquals("ERROR", services.get(0).getStatus());
        assertEquals("Service unreachable.", services.get(0).getStatusMessage());
        assertEquals("RUNNING", services.get(1).getStatus());
        assertEquals("RUNNING", services.get(2).getStatus());
    }

    @Test
    void upfUrlNull_dataPlaneUnknown() {
        testTenant.setUpfMetricsUrl(null);
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(true);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        List<ServiceStatusItem> services = response.getServices();
        assertEquals("RUNNING", services.get(0).getStatus());
        assertEquals("RUNNING", services.get(1).getStatus());
        assertEquals("UNKNOWN", services.get(2).getStatus());
        assertEquals("No URL configured.", services.get(2).getStatusMessage());
        assertTrue(services.get(2).getIpAddresses().isEmpty());
    }

    @Test
    void healthCheckThrowsException_returnsError() {
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenThrow(new RuntimeException("connection refused"));
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);
        when(open5gsClient.isUpfHealthy(UPF_URL)).thenReturn(true);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        assertEquals("ERROR", response.getServices().get(0).getStatus());
        assertEquals("Service unreachable.", response.getServices().get(0).getStatusMessage());
    }

    @Test
    void allUnreachable_allError() {
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(false);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(false);
        when(open5gsClient.isUpfHealthy(UPF_URL)).thenReturn(false);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        List<ServiceStatusItem> services = response.getServices();
        assertAll(
                () -> assertEquals("ERROR", services.get(0).getStatus()),
                () -> assertEquals("ERROR", services.get(1).getStatus()),
                () -> assertEquals("ERROR", services.get(2).getStatus())
        );
    }
}
