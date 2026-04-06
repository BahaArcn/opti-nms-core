package com.opticoms.optinmscore.domain.networkservice.scheduler;

import com.opticoms.optinmscore.domain.networkservice.model.ServiceInstance;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceStatus;
import com.opticoms.optinmscore.domain.networkservice.model.ServiceType;
import com.opticoms.optinmscore.domain.networkservice.service.ServiceInstanceService;
import com.opticoms.optinmscore.domain.observability.model.Alarm;
import com.opticoms.optinmscore.domain.observability.service.AlarmService;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
import com.opticoms.optinmscore.integration.open5gs.Open5gsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceHealthScheduler {

    private final ServiceInstanceService serviceInstanceService;
    private final TenantService tenantService;
    private final Open5gsClient open5gsClient;
    private final AlarmService alarmService;
    private final RestTemplate restTemplate;

    @Scheduled(fixedDelayString = "${networkservice.health.interval-ms:30000}")
    @SchedulerLock(name = "service_health_check", lockAtMostFor = "55s", lockAtLeastFor = "10s")
    public void checkServiceHealth() {
        List<Tenant> tenants = tenantService.getActiveTenants();
        if (tenants.isEmpty()) {
            log.debug("No active tenants, skipping service health check");
            return;
        }

        for (Tenant tenant : tenants) {
            try {
                checkTenantServices(tenant.getTenantId());
            } catch (Exception e) {
                log.error("Service health check failed for tenant {}", tenant.getTenantId(), e);
            }
        }
    }

    private void checkTenantServices(String tenantId) {
        List<ServiceInstance> instances = serviceInstanceService.listByTenant(tenantId);
        for (ServiceInstance instance : instances) {
            try {
                checkSingleInstance(tenantId, instance);
            } catch (Exception e) {
                log.warn("Health check error for service [{}] in tenant {}: {}",
                        instance.getName(), tenantId, e.getMessage());
                serviceInstanceService.updateStatus(instance.getId(), ServiceStatus.ERROR,
                        "Health check error: " + e.getMessage());
            }
        }
    }

    private void checkSingleInstance(String tenantId, ServiceInstance instance) {
        String url = instance.getHealthCheckUrl();
        if (url == null || url.isBlank()) {
            serviceInstanceService.updateStatus(instance.getId(), ServiceStatus.UNKNOWN,
                    "No health check URL configured");
            return;
        }

        boolean healthy = performHealthCheck(instance.getType(), url);

        if (healthy) {
            serviceInstanceService.updateStatus(instance.getId(), ServiceStatus.RUNNING,
                    "The service is running.");
            alarmService.clearAlarm(tenantId, "ServiceHealth", alarmTypeFor(instance));
        } else {
            serviceInstanceService.updateStatus(instance.getId(), ServiceStatus.ERROR,
                    "Service unreachable.");
            raiseServiceAlarm(tenantId, instance);
        }
    }

    private boolean performHealthCheck(ServiceType type, String url) {
        return switch (type) {
            case CONTROL_PLANE -> open5gsClient.isAmfHealthy(url);
            case DATA_PLANE -> open5gsClient.isUpfHealthy(url);
            case POLICY_SUBSCRIBER_MGMT -> plainHttpCheck(url);
        };
    }

    private boolean plainHttpCheck(String url) {
        try {
            var response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private void raiseServiceAlarm(String tenantId, ServiceInstance instance) {
        Alarm alarm = new Alarm();
        alarm.setSource("ServiceHealth");
        alarm.setAlarmType(alarmTypeFor(instance));
        alarm.setSeverity(Alarm.Severity.MAJOR);
        alarm.setDescription("Service " + instance.getName() + " is unreachable");
        alarmService.raiseAlarm(tenantId, alarm);
    }

    private String alarmTypeFor(ServiceInstance instance) {
        return "SERVICE_UNREACHABLE_" + instance.getId();
    }
}
