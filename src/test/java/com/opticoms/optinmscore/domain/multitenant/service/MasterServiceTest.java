package com.opticoms.optinmscore.domain.multitenant.service;

import org.springframework.web.server.ResponseStatusException;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode;
import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode.SlaveStatus;
import com.opticoms.optinmscore.domain.multitenant.repository.SlaveNodeRepository;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.service.NetworkConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterServiceTest {

    @Mock private SlaveNodeRepository slaveNodeRepository;
    @Mock private NetworkConfigService networkConfigService;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private MasterService masterService;

    private static final String TENANT = "OPTC-0001/0001/01";

    private static final String PUBLIC_SLAVE = "http://203.0.113.10:8080";

    @Test
    @DisplayName("registerSlave - saves and returns ONLINE node")
    void registerSlave_savesAndReturnsOnlineNode() {
        when(slaveNodeRepository.findByTenantIdAndSlaveAddress(TENANT, PUBLIC_SLAVE))
                .thenReturn(Optional.empty());
        when(slaveNodeRepository.save(any(SlaveNode.class))).thenAnswer(inv -> inv.getArgument(0));

        SlaveNode result = masterService.registerSlave(TENANT, PUBLIC_SLAVE, "slaveTenant1");

        assertThat(result.getStatus()).isEqualTo(SlaveStatus.ONLINE);
        assertThat(result.getSlaveAddress()).isEqualTo(PUBLIC_SLAVE);
        assertThat(result.getSlaveTenantId()).isEqualTo("slaveTenant1");
        assertThat(result.getRegisteredAt()).isNotNull();
        assertThat(result.getLastHeartbeat()).isNotNull();
        verify(slaveNodeRepository).save(any(SlaveNode.class));
    }

    @Test
    @DisplayName("registerSlave - when duplicate, throws ResponseStatusException CONFLICT")
    void registerSlave_whenDuplicate_throwsResponseStatusException() {
        when(slaveNodeRepository.findByTenantIdAndSlaveAddress(TENANT, PUBLIC_SLAVE))
                .thenReturn(Optional.of(new SlaveNode()));

        assertThatThrownBy(() -> masterService.registerSlave(TENANT, PUBLIC_SLAVE, "slaveTenant1"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("deregisterSlave - when not found, throws ResponseStatusException NOT_FOUND")
    void deregisterSlave_whenNotFound_throwsResponseStatusException() {
        when(slaveNodeRepository.findByTenantIdAndSlaveAddress(TENANT, "http://unknown:8080"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> masterService.deregisterSlave(TENANT, "http://unknown:8080"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("heartbeat - updates lastHeartbeat and status")
    void heartbeat_updatesLastHeartbeatAndStatus() {
        SlaveNode node = new SlaveNode();
        node.setSlaveAddress("http://slave1:8080");
        node.setStatus(SlaveStatus.OFFLINE);
        when(slaveNodeRepository.findByTenantIdAndSlaveAddress(TENANT, "http://slave1:8080"))
                .thenReturn(Optional.of(node));
        when(slaveNodeRepository.save(any(SlaveNode.class))).thenAnswer(inv -> inv.getArgument(0));

        masterService.heartbeat(TENANT, "http://slave1:8080");

        assertThat(node.getStatus()).isEqualTo(SlaveStatus.ONLINE);
        assertThat(node.getLastHeartbeat()).isNotNull();
        verify(slaveNodeRepository).save(node);
    }

    @Test
    @DisplayName("pushConfigToAllSlaves - calls online slaves")
    void pushConfigToAllSlaves_callsOnlineSlaves() {
        GlobalConfig config = new GlobalConfig();
        config.setNetworkFullName("TestNet");
        config.setNetworkShortName("TN");
        config.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);
        when(networkConfigService.getGlobalConfig(TENANT)).thenReturn(config);

        SlaveNode slave1 = new SlaveNode();
        slave1.setSlaveAddress("http://slave1:8080");
        slave1.setSlaveTenantId("st1");
        slave1.setStatus(SlaveStatus.ONLINE);

        SlaveNode slave2 = new SlaveNode();
        slave2.setSlaveAddress("http://slave2:8080");
        slave2.setSlaveTenantId("st2");
        slave2.setStatus(SlaveStatus.ONLINE);

        when(slaveNodeRepository.findByTenantIdAndStatus(eq(TENANT), eq(SlaveStatus.ONLINE), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(slave1, slave2)));

        masterService.pushConfigToAllSlaves(TENANT);

        verify(restTemplate, times(2)).exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    @DisplayName("pushConfigToAllSlaves - when slave fails, continues other slaves")
    void pushConfigToAllSlaves_whenSlaveFails_continuesOtherSlaves() {
        GlobalConfig config = new GlobalConfig();
        config.setNetworkFullName("TestNet");
        config.setNetworkShortName("TN");
        config.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);
        when(networkConfigService.getGlobalConfig(TENANT)).thenReturn(config);

        SlaveNode slave1 = new SlaveNode();
        slave1.setSlaveAddress("http://slave1:8080");
        slave1.setSlaveTenantId("st1");
        slave1.setStatus(SlaveStatus.ONLINE);

        SlaveNode slave2 = new SlaveNode();
        slave2.setSlaveAddress("http://slave2:8080");
        slave2.setSlaveTenantId("st2");
        slave2.setStatus(SlaveStatus.ONLINE);

        when(slaveNodeRepository.findByTenantIdAndStatus(eq(TENANT), eq(SlaveStatus.ONLINE), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(slave1, slave2)));

        when(restTemplate.exchange(
                eq("http://slave1:8080/api/v1/slave/config"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("Connection refused"));

        masterService.pushConfigToAllSlaves(TENANT);

        assertThat(slave1.getStatus()).isEqualTo(SlaveStatus.OFFLINE);
        verify(slaveNodeRepository).saveAll(anyList());
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    @DisplayName("markStaleSlaves - sets OFFLINE status for stale nodes")
    void markStaleSlaves_setsOfflineStatusForStaleNodes() {
        SlaveNode stale = new SlaveNode();
        stale.setSlaveAddress("http://slave-stale:8080");
        stale.setStatus(SlaveStatus.ONLINE);
        stale.setLastHeartbeat(new Date(System.currentTimeMillis() - 180_000)); // 3 min ago

        SlaveNode fresh = new SlaveNode();
        fresh.setSlaveAddress("http://slave-fresh:8080");
        fresh.setStatus(SlaveStatus.ONLINE);
        fresh.setLastHeartbeat(new Date()); // now

        when(slaveNodeRepository.findByTenantIdAndStatus(TENANT, SlaveStatus.ONLINE))
                .thenReturn(List.of(stale, fresh));

        int count = masterService.markStaleSlaves(TENANT);

        assertThat(count).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(SlaveStatus.OFFLINE);
        assertThat(fresh.getStatus()).isEqualTo(SlaveStatus.ONLINE);
        verify(slaveNodeRepository, times(1)).saveAll(List.of(stale));
    }
}
