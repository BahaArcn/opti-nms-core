package com.opticoms.optinmscore.domain.networkservice.service;

import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse.ServiceStatusItem;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
import com.opticoms.optinmscore.integration.open5gs.Open5gsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkOverviewService {

    private final TenantService tenantService;
    private final Open5gsClient open5gsClient;

    public NetworkOverviewResponse getOverview(String tenantId) {
        Tenant tenant = tenantService.getTenant(tenantId);

        ServiceStatusItem controlPlane = checkService(
                "Control Plane", "CONTROL_PLANE",
                tenant.getAmfUrl(),
                url -> open5gsClient.isAmfHealthy(url));

        ServiceStatusItem policySubscriber = checkService(
                "Policy & Subscriber Mgmt.", "POLICY_SUBSCRIBER_MGMT",
                tenant.getSmfUrl(),
                url -> open5gsClient.isSmfHealthy(url));

        ServiceStatusItem dataPlane = checkService(
                "Data Plane", "DATA_PLANE",
                tenant.getUpfMetricsUrl(),
                url -> open5gsClient.isUpfHealthy(url));

        return NetworkOverviewResponse.builder()
                .services(List.of(controlPlane, policySubscriber, dataPlane))
                .build();
    }

    private ServiceStatusItem checkService(String name, String type, String url,
                                           java.util.function.Function<String, Boolean> healthCheck) {
        List<String> ips = parseIp(url);
        if (url == null || url.isBlank()) {
            return ServiceStatusItem.builder()
                    .name(name).type(type)
                    .status("UNKNOWN").statusMessage("No URL configured.")
                    .ipAddresses(Collections.emptyList())
                    .build();
        }
        try {
            boolean healthy = healthCheck.apply(url);
            return ServiceStatusItem.builder()
                    .name(name).type(type)
                    .status(healthy ? "RUNNING" : "ERROR")
                    .statusMessage(healthy ? "The service is running." : "Service unreachable.")
                    .ipAddresses(ips)
                    .build();
        } catch (Exception e) {
            log.warn("Health check failed for {} ({}): {}", name, url, e.getMessage());
            return ServiceStatusItem.builder()
                    .name(name).type(type)
                    .status("ERROR").statusMessage("Service unreachable.")
                    .ipAddresses(ips)
                    .build();
        }
    }

    private List<String> parseIp(String url) {
        if (url == null || url.isBlank()) return Collections.emptyList();
        try {
            String host = URI.create(url).getHost();
            return (host != null && !host.isBlank()) ? List.of(host) : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
