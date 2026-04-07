package com.opticoms.optinmscore.domain.networkservice.service;

import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse.NfStatus;
import com.opticoms.optinmscore.domain.networkservice.dto.NetworkOverviewResponse.ServiceStatusItem;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
import com.opticoms.optinmscore.integration.open5gs.Open5gsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkOverviewService {

    private final TenantService tenantService;
    private final Open5gsClient open5gsClient;

    public NetworkOverviewResponse getOverview(String tenantId) {
        Tenant tenant = tenantService.getTenant(tenantId);

        // Define NFs per group with their URLs and health check suppliers
        Map<String, Supplier<Boolean>> controlPlaneNfs = new LinkedHashMap<>();
        controlPlaneNfs.put("AMF", () -> safeCheck(() -> open5gsClient.isAmfHealthy(tenant.getAmfUrl()), tenant.getAmfUrl()));
        controlPlaneNfs.put("SMF", () -> safeCheck(() -> open5gsClient.isSmfHealthy(tenant.getSmfUrl()), tenant.getSmfUrl()));
        controlPlaneNfs.put("NRF", () -> safeCheck(() -> open5gsClient.isNfHealthy(tenant.getNrfUrl()), tenant.getNrfUrl()));
        controlPlaneNfs.put("NSSF", () -> safeCheck(() -> open5gsClient.isNfHealthy(tenant.getNssfUrl()), tenant.getNssfUrl()));
        controlPlaneNfs.put("SCP", () -> safeCheck(() -> open5gsClient.isNfHealthy(tenant.getScpUrl()), tenant.getScpUrl()));
        controlPlaneNfs.put("AUSF", () -> safeCheck(() -> open5gsClient.isNfHealthy(tenant.getAusfUrl()), tenant.getAusfUrl()));
        controlPlaneNfs.put("UDM", () -> safeCheck(() -> open5gsClient.isNfHealthy(tenant.getUdmUrl()), tenant.getUdmUrl()));
        controlPlaneNfs.put("UDR", () -> safeCheck(() -> open5gsClient.isNfHealthy(tenant.getUdrUrl()), tenant.getUdrUrl()));
        controlPlaneNfs.put("BSF", () -> safeCheck(() -> open5gsClient.isNfHealthy(tenant.getBsfUrl()), tenant.getBsfUrl()));

        Map<String, String> controlPlaneUrls = new LinkedHashMap<>();
        controlPlaneUrls.put("AMF", tenant.getAmfUrl());
        controlPlaneUrls.put("SMF", tenant.getSmfUrl());
        controlPlaneUrls.put("NRF", tenant.getNrfUrl());
        controlPlaneUrls.put("NSSF", tenant.getNssfUrl());
        controlPlaneUrls.put("SCP", tenant.getScpUrl());
        controlPlaneUrls.put("AUSF", tenant.getAusfUrl());
        controlPlaneUrls.put("UDM", tenant.getUdmUrl());
        controlPlaneUrls.put("UDR", tenant.getUdrUrl());
        controlPlaneUrls.put("BSF", tenant.getBsfUrl());

        Map<String, Supplier<Boolean>> policyNfs = new LinkedHashMap<>();
        policyNfs.put("PCF", () -> safeCheck(() -> open5gsClient.isNfHealthy(tenant.getPcfUrl()), tenant.getPcfUrl()));

        Map<String, String> policyUrls = new LinkedHashMap<>();
        policyUrls.put("PCF", tenant.getPcfUrl());

        Map<String, Supplier<Boolean>> dataPlaneNfs = new LinkedHashMap<>();
        dataPlaneNfs.put("UPF", () -> safeCheck(() -> open5gsClient.isUpfHealthy(tenant.getUpfMetricsUrl()), tenant.getUpfMetricsUrl()));

        Map<String, String> dataPlaneUrls = new LinkedHashMap<>();
        dataPlaneUrls.put("UPF", tenant.getUpfMetricsUrl());

        // Launch all health checks in parallel
        Map<String, CompletableFuture<Boolean>> allFutures = new LinkedHashMap<>();
        controlPlaneNfs.forEach((name, check) -> allFutures.put("CP_" + name, CompletableFuture.supplyAsync(check)));
        policyNfs.forEach((name, check) -> allFutures.put("PSM_" + name, CompletableFuture.supplyAsync(check)));
        dataPlaneNfs.forEach((name, check) -> allFutures.put("DP_" + name, CompletableFuture.supplyAsync(check)));

        CompletableFuture.allOf(allFutures.values().toArray(new CompletableFuture[0])).join();

        // Build groups from results
        ServiceStatusItem controlPlane = buildGroup(
                "Control Plane", "CONTROL_PLANE",
                List.of("AMF", "SMF", "NRF", "NSSF", "SCP", "AUSF", "UDM", "UDR", "BSF"),
                "CP_", allFutures, controlPlaneUrls);

        ServiceStatusItem policySubscriber = buildGroup(
                "Policy & Subscriber Mgmt.", "POLICY_SUBSCRIBER_MGMT",
                List.of("PCF"),
                "PSM_", allFutures, policyUrls);

        ServiceStatusItem dataPlane = buildGroup(
                "Data Plane", "DATA_PLANE",
                List.of("UPF"),
                "DP_", allFutures, dataPlaneUrls);

        return NetworkOverviewResponse.builder()
                .services(List.of(controlPlane, policySubscriber, dataPlane))
                .build();
    }

    private ServiceStatusItem buildGroup(String groupName, String groupType,
                                          List<String> nfNames, String futurePrefix,
                                          Map<String, CompletableFuture<Boolean>> allFutures,
                                          Map<String, String> urlMap) {
        List<NfStatus> components = new ArrayList<>();
        int configured = 0;
        int running = 0;
        List<String> downNames = new ArrayList<>();

        for (String nfName : nfNames) {
            String url = urlMap.get(nfName);
            String ip = parseIp(url);

            if (url == null || url.isBlank()) {
                components.add(NfStatus.builder()
                        .name(nfName).status("UNKNOWN").ipAddress(null)
                        .build());
                continue;
            }

            configured++;
            boolean healthy = allFutures.get(futurePrefix + nfName).join();

            if (healthy) {
                running++;
                components.add(NfStatus.builder()
                        .name(nfName).status("RUNNING").ipAddress(ip)
                        .build());
            } else {
                downNames.add(nfName);
                components.add(NfStatus.builder()
                        .name(nfName).status("ERROR").ipAddress(ip)
                        .build());
            }
        }

        String status;
        String message;
        if (configured == 0) {
            status = "UNKNOWN";
            message = "No URL configured.";
        } else if (running == configured) {
            status = "RUNNING";
            message = configured == 1
                    ? "The service is running."
                    : "All " + configured + " services running.";
        } else if (running == 0) {
            status = "ERROR";
            message = configured == 1
                    ? "Service unreachable."
                    : "All " + configured + " services down.";
        } else {
            status = "DEGRADED";
            message = downNames.size() + "/" + configured + " down: " + String.join(", ", downNames);
        }

        return ServiceStatusItem.builder()
                .name(groupName).type(groupType)
                .status(status).statusMessage(message)
                .components(components)
                .build();
    }

    private boolean safeCheck(Supplier<Boolean> check, String url) {
        if (url == null || url.isBlank()) return false;
        try {
            return check.get();
        } catch (Exception e) {
            log.warn("Health check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }

    private String parseIp(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String host = URI.create(url).getHost();
            return (host != null && !host.isBlank()) ? host : null;
        } catch (Exception e) {
            return null;
        }
    }
}
