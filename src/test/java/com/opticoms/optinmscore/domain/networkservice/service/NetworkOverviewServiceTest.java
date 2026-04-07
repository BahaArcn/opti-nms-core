package com.opticoms.optinmscore.domain.networkservice.service;

import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse.NfStatus;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkOverviewServiceTest {

    private static final String TENANT_ID = "OPTC-0001/0001/01";
    private static final String AMF_URL = "http://10.244.0.129:9090";
    private static final String SMF_URL = "http://10.244.0.134:9090";
    private static final String UPF_URL = "http://10.244.0.140:9090/metrics";
    private static final String NRF_URL = "http://10.244.0.131:7777";
    private static final String NSSF_URL = "http://10.244.0.133:7777";
    private static final String SCP_URL = "http://10.244.0.135:7777";
    private static final String AUSF_URL = "http://10.244.0.132:7777";
    private static final String UDM_URL = "http://10.244.0.136:7777";
    private static final String UDR_URL = "http://10.244.0.137:7777";
    private static final String BSF_URL = "http://10.244.0.130:7777";
    private static final String PCF_URL = "http://10.244.0.138:7777";

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
        testTenant.setNrfUrl(NRF_URL);
        testTenant.setNssfUrl(NSSF_URL);
        testTenant.setScpUrl(SCP_URL);
        testTenant.setAusfUrl(AUSF_URL);
        testTenant.setUdmUrl(UDM_URL);
        testTenant.setUdrUrl(UDR_URL);
        testTenant.setBsfUrl(BSF_URL);
        testTenant.setPcfUrl(PCF_URL);
        when(tenantService.getTenant(TENANT_ID)).thenReturn(testTenant);
    }

    private void stubAllHealthy() {
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(true);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);
        when(open5gsClient.isUpfHealthy(UPF_URL)).thenReturn(true);
        when(open5gsClient.isNfHealthy(anyString())).thenReturn(true);
    }

    @Test
    void allHealthy_returnsThreeRunningGroups() {
        stubAllHealthy();

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        List<ServiceStatusItem> services = response.getServices();
        assertEquals(3, services.size());

        ServiceStatusItem cp = services.get(0);
        assertEquals("RUNNING", cp.getStatus());
        assertEquals("CONTROL_PLANE", cp.getType());
        assertEquals("All 9 services running.", cp.getStatusMessage());
        assertEquals(9, cp.getComponents().size());
        assertTrue(cp.getComponents().stream().allMatch(c -> "RUNNING".equals(c.getStatus())));

        ServiceStatusItem psm = services.get(1);
        assertEquals("RUNNING", psm.getStatus());
        assertEquals("POLICY_SUBSCRIBER_MGMT", psm.getType());
        assertEquals("The service is running.", psm.getStatusMessage());
        assertEquals(1, psm.getComponents().size());
        assertEquals("PCF", psm.getComponents().get(0).getName());

        ServiceStatusItem dp = services.get(2);
        assertEquals("RUNNING", dp.getStatus());
        assertEquals("DATA_PLANE", dp.getType());
        assertEquals("The service is running.", dp.getStatusMessage());
        assertEquals(1, dp.getComponents().size());
        assertEquals("UPF", dp.getComponents().get(0).getName());
    }

    @Test
    void controlPlane_someDown_returnsDegraded() {
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(true);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);
        when(open5gsClient.isNfHealthy(anyString())).thenReturn(true);
        when(open5gsClient.isNfHealthy(NRF_URL)).thenReturn(false);
        when(open5gsClient.isNfHealthy(BSF_URL)).thenReturn(false);
        when(open5gsClient.isUpfHealthy(UPF_URL)).thenReturn(true);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        ServiceStatusItem cp = response.getServices().get(0);
        assertEquals("DEGRADED", cp.getStatus());
        assertEquals("2/9 down: NRF, BSF", cp.getStatusMessage());

        List<NfStatus> components = cp.getComponents();
        assertEquals("RUNNING", components.stream().filter(c -> "AMF".equals(c.getName())).findFirst().get().getStatus());
        assertEquals("ERROR", components.stream().filter(c -> "NRF".equals(c.getName())).findFirst().get().getStatus());
        assertEquals("ERROR", components.stream().filter(c -> "BSF".equals(c.getName())).findFirst().get().getStatus());
    }

    @Test
    void controlPlane_allDown_returnsError() {
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(false);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(false);
        when(open5gsClient.isNfHealthy(anyString())).thenReturn(false);
        when(open5gsClient.isUpfHealthy(UPF_URL)).thenReturn(true);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        ServiceStatusItem cp = response.getServices().get(0);
        assertEquals("ERROR", cp.getStatus());
        assertEquals("All 9 services down.", cp.getStatusMessage());
    }

    @Test
    void noUrlsConfigured_returnsUnknown() {
        testTenant.setAmfUrl(null);
        testTenant.setSmfUrl(null);
        testTenant.setUpfMetricsUrl(null);
        testTenant.setNrfUrl(null);
        testTenant.setNssfUrl(null);
        testTenant.setScpUrl(null);
        testTenant.setAusfUrl(null);
        testTenant.setUdmUrl(null);
        testTenant.setUdrUrl(null);
        testTenant.setBsfUrl(null);
        testTenant.setPcfUrl(null);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        List<ServiceStatusItem> services = response.getServices();
        assertAll(
                () -> assertEquals("UNKNOWN", services.get(0).getStatus()),
                () -> assertEquals("No URL configured.", services.get(0).getStatusMessage()),
                () -> assertEquals("UNKNOWN", services.get(1).getStatus()),
                () -> assertEquals("UNKNOWN", services.get(2).getStatus())
        );
        assertTrue(services.get(0).getComponents().stream().allMatch(c -> "UNKNOWN".equals(c.getStatus())));
    }

    @Test
    void upfDown_dataPlaneError() {
        stubAllHealthy();
        when(open5gsClient.isUpfHealthy(UPF_URL)).thenReturn(false);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        ServiceStatusItem dp = response.getServices().get(2);
        assertEquals("ERROR", dp.getStatus());
        assertEquals("Service unreachable.", dp.getStatusMessage());
        assertEquals("ERROR", dp.getComponents().get(0).getStatus());
    }

    @Test
    void pcfDown_policyError() {
        stubAllHealthy();
        when(open5gsClient.isNfHealthy(PCF_URL)).thenReturn(false);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        ServiceStatusItem psm = response.getServices().get(1);
        assertEquals("ERROR", psm.getStatus());
        assertEquals("Service unreachable.", psm.getStatusMessage());
    }

    @Test
    void healthCheckThrowsException_treatedAsDown() {
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenThrow(new RuntimeException("connection refused"));
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);
        when(open5gsClient.isNfHealthy(anyString())).thenReturn(true);
        when(open5gsClient.isUpfHealthy(UPF_URL)).thenReturn(true);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        ServiceStatusItem cp = response.getServices().get(0);
        assertEquals("DEGRADED", cp.getStatus());
        assertTrue(cp.getStatusMessage().contains("AMF"));
    }

    @Test
    void componentIpAddresses_parsedFromUrls() {
        stubAllHealthy();

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        List<NfStatus> cpComponents = response.getServices().get(0).getComponents();
        assertEquals("10.244.0.129", cpComponents.stream().filter(c -> "AMF".equals(c.getName())).findFirst().get().getIpAddress());
        assertEquals("10.244.0.131", cpComponents.stream().filter(c -> "NRF".equals(c.getName())).findFirst().get().getIpAddress());

        NfStatus pcf = response.getServices().get(1).getComponents().get(0);
        assertEquals("10.244.0.138", pcf.getIpAddress());

        NfStatus upf = response.getServices().get(2).getComponents().get(0);
        assertEquals("10.244.0.140", upf.getIpAddress());
    }

    @Test
    void upfUrlNull_dataPlaneUnknown() {
        testTenant.setUpfMetricsUrl(null);
        when(open5gsClient.isAmfHealthy(AMF_URL)).thenReturn(true);
        when(open5gsClient.isSmfHealthy(SMF_URL)).thenReturn(true);
        when(open5gsClient.isNfHealthy(anyString())).thenReturn(true);

        NetworkOverviewResponse response = service.getOverview(TENANT_ID);

        ServiceStatusItem dp = response.getServices().get(2);
        assertEquals("UNKNOWN", dp.getStatus());
        assertEquals("No URL configured.", dp.getStatusMessage());
        assertNull(dp.getComponents().get(0).getIpAddress());
    }
}
