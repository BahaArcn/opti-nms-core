package com.opticoms.optinmscore.domain.multitenant.service;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
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
        validateSlaveAddress(slaveAddress);
        if (slaveNodeRepository.findByTenantIdAndSlaveAddress(tenantId, slaveAddress).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slave already registered at this address");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Slave not found at this address"));
        log.info("Deregistering slave: address={}, tenant={}", slaveAddress, tenantId);
        slaveNodeRepository.delete(node);
    }

    public void heartbeat(String tenantId, String slaveAddress) {
        SlaveNode node = slaveNodeRepository.findByTenantIdAndSlaveAddress(tenantId, slaveAddress)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Slave not found at this address"));
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

        int pageNum = 0;
        int pageSize = 50;
        List<SlaveNode> failedSlaves = new ArrayList<>();
        Page<SlaveNode> page;
        do {
            page = slaveNodeRepository.findByTenantIdAndStatus(
                    tenantId, SlaveStatus.ONLINE, PageRequest.of(pageNum++, pageSize));

            for (SlaveNode slave : page.getContent()) {
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
                    failedSlaves.add(slave);
                }
            }
        } while (page.hasNext());

        if (!failedSlaves.isEmpty()) {
            slaveNodeRepository.saveAll(failedSlaves);
        }
    }

    public int markStaleSlaves(String tenantId) {
        List<SlaveNode> onlineSlaves = slaveNodeRepository.findByTenantIdAndStatus(tenantId, SlaveStatus.ONLINE);
        long cutoff = System.currentTimeMillis() - 120_000;
        List<SlaveNode> toUpdate = new ArrayList<>();

        for (SlaveNode slave : onlineSlaves) {
            if (slave.getLastHeartbeat() != null && slave.getLastHeartbeat().getTime() < cutoff) {
                slave.setStatus(SlaveStatus.OFFLINE);
                toUpdate.add(slave);
                log.info("Marked slave as OFFLINE (stale): {}", slave.getSlaveAddress());
            }
        }

        if (!toUpdate.isEmpty()) {
            slaveNodeRepository.saveAll(toUpdate);
        }
        return toUpdate.size();
    }

    private void validateSlaveAddress(String address) {
        try {
            URI uri = URI.create(address);
            String host = uri.getHost();
            if (host == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid slave address");
            }
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Slave address cannot be a private/loopback IP");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid slave address");
        }
    }
}
