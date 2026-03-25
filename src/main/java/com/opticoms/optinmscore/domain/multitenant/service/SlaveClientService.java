package com.opticoms.optinmscore.domain.multitenant.service;

import com.opticoms.optinmscore.domain.multitenant.model.MultiTenantConfigPayload;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import com.opticoms.optinmscore.domain.network.service.NetworkConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaveClientService {

    private final NetworkConfigService networkConfigService;
    private final GlobalConfigRepository globalConfigRepository;
    private final RestTemplate restTemplate;

    @Value("${app.self-address:http://localhost:8080}")
    private String selfAddress;

    @Value("${app.master-token}")
    private String masterToken;

    @EventListener(ApplicationReadyEvent.class)
    public void registerWithMaster() {
        List<GlobalConfig> configs = globalConfigRepository.findAll();
        for (GlobalConfig config : configs) {
            if (!config.isWorkAsMaster() && config.getMasterAddr() != null && !config.getMasterAddr().isBlank()) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-Master-Token", masterToken);
                    headers.set("X-Tenant-ID", config.getTenantId());
                    headers.set("Content-Type", "application/json");

                    Map<String, String> body = Map.of(
                            "slaveAddress", selfAddress,
                            "slaveTenantId", config.getTenantId()
                    );

                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
                    restTemplate.exchange(
                            config.getMasterAddr() + "/api/v1/master/slaves/register",
                            HttpMethod.POST,
                            entity,
                            Void.class
                    );
                    log.info("Registered with master {} for tenant={}", config.getMasterAddr(), config.getTenantId());
                } catch (Exception e) {
                    log.warn("Failed to register with master {}: {}", config.getMasterAddr(), e.getMessage());
                }
            }
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void sendHeartbeat() {
        List<GlobalConfig> configs = globalConfigRepository.findAll();
        for (GlobalConfig config : configs) {
            if (!config.isWorkAsMaster() && config.getMasterAddr() != null && !config.getMasterAddr().isBlank()) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-Master-Token", masterToken);
                    headers.set("X-Tenant-ID", config.getTenantId());
                    headers.set("Content-Type", "application/json");

                    Map<String, String> body = Map.of("slaveAddress", selfAddress);

                    HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
                    restTemplate.exchange(
                            config.getMasterAddr() + "/api/v1/master/slaves/heartbeat",
                            HttpMethod.POST,
                            entity,
                            Void.class
                    );
                    log.debug("Heartbeat sent to master {} for tenant={}", config.getMasterAddr(), config.getTenantId());
                } catch (Exception e) {
                    log.debug("Heartbeat failed for master {}: {}", config.getMasterAddr(), e.getMessage());
                }
            }
        }
    }

    public void applyConfigFromMaster(String tenantId, MultiTenantConfigPayload payload) {
        GlobalConfig config;
        try {
            config = networkConfigService.getGlobalConfig(tenantId);
        } catch (Exception e) {
            log.warn("Cannot apply config from master: GlobalConfig not found for tenant={}", tenantId);
            return;
        }

        config.setNetworkFullName(payload.getNetworkFullName());
        config.setNetworkShortName(payload.getNetworkShortName());
        config.setNetworkMode(payload.getNetworkMode());

        globalConfigRepository.save(config);
        log.info("Applied config from master for tenant={}: fullName={}, shortName={}, mode={}",
                tenantId, payload.getNetworkFullName(), payload.getNetworkShortName(), payload.getNetworkMode());
    }

    public String getSelfAddress() {
        return selfAddress;
    }
}
