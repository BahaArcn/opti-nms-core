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
import com.opticoms.optinmscore.domain.tenant.repository.TenantRepository;
import com.opticoms.optinmscore.integration.open5gs.Open5gsClient;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubscriberRepository subscriberRepository;
    private final GNodeBRepository gNodeBRepository;
    private final ConnectedUeRepository connectedUeRepository;
    private final PduSessionRepository pduSessionRepository;
    private final AlarmRepository alarmRepository;
    private final Open5gsClient open5gsClient;
    private final TenantRepository tenantRepository;
    private final EdgeLocationRepository edgeLocationRepository;
    private final LicenseService licenseService;

    public DashboardSummary getDashboardSummary(String tenantId) {
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant not found: " + tenantId));

        boolean amfReachable = open5gsClient.isAmfHealthy(tenant.getAmfUrl());
        boolean smfReachable = open5gsClient.isSmfHealthy(tenant.getSmfUrl());
        boolean upfReachable = tenant.getUpfMetricsUrl() != null
                && open5gsClient.isUpfHealthy(tenant.getUpfMetricsUrl());

        long criticalAlarms = alarmRepository.countByTenantIdAndSeverityAndStatus(
                tenantId, Alarm.Severity.CRITICAL, Alarm.AlarmStatus.ACTIVE);
        long majorAlarms = alarmRepository.countByTenantIdAndSeverityAndStatus(
                tenantId, Alarm.Severity.MAJOR, Alarm.AlarmStatus.ACTIVE);

        return DashboardSummary.builder()
                .totalSubscribers(subscriberRepository.countByTenantId(tenantId))
                .totalGNodeBs(gNodeBRepository.countByTenantId(tenantId))
                .connectedGNodeBs(gNodeBRepository.countByTenantIdAndStatus(tenantId, GNodeB.ConnectionStatus.CONNECTED))
                .disconnectedGNodeBs(gNodeBRepository.countByTenantIdAndStatus(tenantId, GNodeB.ConnectionStatus.DISCONNECTED))
                .connectedUes(connectedUeRepository.countByTenantIdAndStatus(tenantId, ConnectedUe.UeStatus.CONNECTED))
                .idleUes(connectedUeRepository.countByTenantIdAndStatus(tenantId, ConnectedUe.UeStatus.IDLE))
                .totalUes(connectedUeRepository.countByTenantId(tenantId))
                .activeSessions(pduSessionRepository.countByTenantIdAndStatus(tenantId, PduSession.SessionStatus.ACTIVE))
                .activeAlarms(alarmRepository.countByTenantIdAndStatus(tenantId, Alarm.AlarmStatus.ACTIVE))
                .totalEdgeLocations(edgeLocationRepository.countByTenantId(tenantId))
                .criticalAlarms(criticalAlarms)
                .majorAlarms(majorAlarms)
                .systemStatus(determineSystemStatus(amfReachable, smfReachable, criticalAlarms, majorAlarms))
                .amfReachable(amfReachable)
                .smfReachable(smfReachable)
                .upfReachable(upfReachable)
                .licenseActive(isLicenseActive(tenantId))
                .build();
    }

    private boolean isLicenseActive(String tenantId) {
        try {
            LicenseService.LicenseStatus status = licenseService.getLicenseStatus(tenantId);
            return status.isActive();
        } catch (Exception e) {
            return true; // no license = unrestricted
        }
    }

    private SystemStatus determineSystemStatus(boolean amfHealthy, boolean smfHealthy,
                                               long criticalAlarms, long majorAlarms) {
        if (criticalAlarms > 0 || (!amfHealthy && !smfHealthy)) {
            return SystemStatus.DOWN;
        } else if (majorAlarms > 0 || !amfHealthy || !smfHealthy) {
            return SystemStatus.DEGRADED;
        }
        return SystemStatus.HEALTHY;
    }

    @Data
    @Builder
    public static class DashboardSummary {
        private long totalSubscribers;
        private long totalGNodeBs;
        private long connectedGNodeBs;
        private long disconnectedGNodeBs;
        private long connectedUes;
        private long idleUes;
        private long totalUes;
        private long activeSessions;
        private long activeAlarms;
        private long totalEdgeLocations;
        private long criticalAlarms;
        private long majorAlarms;
        private SystemStatus systemStatus;
        private boolean amfReachable;
        private boolean smfReachable;
        private boolean upfReachable;
        private boolean licenseActive;
    }

    public enum SystemStatus {
        HEALTHY, DEGRADED, DOWN
    }
}
