package com.opticoms.optinmscore.domain.multitenant.service;

import com.opticoms.optinmscore.common.exception.EntityNotFoundException;
import com.opticoms.optinmscore.domain.multitenant.model.MultiTenantConfigPayload;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode.SlaveStatus;
import com.opticoms.optinmscore.domain.multitenant.repository.SlaveNodeRepository;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.service.NetworkConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MasterService {

    private final SlaveNodeRepository slaveNodeRepository;
    private final NetworkConfigService networkConfigService;
    private final RestTemplate restTemplate;

    @Value("${app.master-token}")
    private String masterToken;

    public SlaveNode registerSlave(String tenantId, String slaveAddress, String slaveTenantId) {
        if (slaveNodeRepository.findByTenantIdAndSlaveAddress(tenantId, slaveAddress).isPresent()) {
            throw new IllegalStateException("Slave already registered: " + slaveAddress);
        }

        SlaveNode node = new SlaveNode();
        node.setTenantId(tenantId);
        node.setSlaveAddress(slaveAddress);
        node.setSlaveTenantId(slaveTenantId);
        node.setStatus(SlaveStatus.ONLINE);
        node.setRegisteredAt(new Date());
        node.setLastHeartbeat(new Date());

        log.info("Registering slave: address={}, slaveTenant={}, masterTenant={}",
                slaveAddress, slaveTenantId, tenantId);
        return slaveNodeRepository.save(node);
    }

    public void deregisterSlave(String tenantId, String slaveAddress) {
        SlaveNode node = slaveNodeRepository.findByTenantIdAndSlaveAddress(tenantId, slaveAddress)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Slave not found: " + slaveAddress));
        log.info("Deregistering slave: address={}, tenant={}", slaveAddress, tenantId);
        slaveNodeRepository.delete(node);
    }

    public void heartbeat(String tenantId, String slaveAddress) {
        SlaveNode node = slaveNodeRepository.findByTenantIdAndSlaveAddress(tenantId, slaveAddress)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Slave not found: " + slaveAddress));
        node.setLastHeartbeat(new Date());
        node.setStatus(SlaveStatus.ONLINE);
        slaveNodeRepository.save(node);
    }

    public Page<SlaveNode> listSlaves(String tenantId, Pageable pageable) {
        return slaveNodeRepository.findByTenantId(tenantId, pageable);
    }

    public void pushConfigToAllSlaves(String tenantId) {
        GlobalConfig config = networkConfigService.getGlobalConfig(tenantId);

        MultiTenantConfigPayload payload = new MultiTenantConfigPayload();
        payload.setNetworkFullName(config.getNetworkFullName());
        payload.setNetworkShortName(config.getNetworkShortName());
        payload.setNetworkMode(config.getNetworkMode());

        List<SlaveNode> onlineSlaves = slaveNodeRepository.findByTenantIdAndStatus(tenantId, SlaveStatus.ONLINE);
        log.info("Pushing config to {} online slave(s) for tenant={}", onlineSlaves.size(), tenantId);

        for (SlaveNode slave : onlineSlaves) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Master-Token", masterToken);
                headers.set("X-Tenant-ID", slave.getSlaveTenantId());
                headers.set("Content-Type", "application/json");

                HttpEntity<MultiTenantConfigPayload> entity = new HttpEntity<>(payload, headers);
                restTemplate.exchange(
                        slave.getSlaveAddress() + "/api/v1/slave/config",
                        HttpMethod.POST,
                        entity,
                        Void.class
                );
                log.info("Config pushed to slave: {}", slave.getSlaveAddress());
            } catch (Exception e) {
                log.warn("Failed to push config to slave {}: {}", slave.getSlaveAddress(), e.getMessage());
                slave.setStatus(SlaveStatus.OFFLINE);
                slaveNodeRepository.save(slave);
            }
        }
    }

    public int markStaleSlaves(String tenantId) {
        List<SlaveNode> onlineSlaves = slaveNodeRepository.findByTenantIdAndStatus(tenantId, SlaveStatus.ONLINE);
        long cutoff = System.currentTimeMillis() - 120_000; // 2 minutes
        int count = 0;

        for (SlaveNode slave : onlineSlaves) {
            if (slave.getLastHeartbeat() != null && slave.getLastHeartbeat().getTime() < cutoff) {
                slave.setStatus(SlaveStatus.OFFLINE);
                slaveNodeRepository.save(slave);
                log.info("Marked slave as OFFLINE (stale): {}", slave.getSlaveAddress());
                count++;
            }
        }
        return count;
    }
}
