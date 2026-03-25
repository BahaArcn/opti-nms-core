package com.opticoms.optinmscore.domain.multitenant.service;

import com.opticoms.optinmscore.domain.multitenant.model.MultiTenantConfigPayload;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import com.opticoms.optinmscore.domain.network.service.NetworkConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaveClientServiceTest {

    @Mock private NetworkConfigService networkConfigService;
    @Mock private GlobalConfigRepository globalConfigRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private SlaveClientService slaveClientService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(slaveClientService, "selfAddress", "http://localhost:8080");
        ReflectionTestUtils.setField(slaveClientService, "masterToken", "test-master-token");
    }

    private GlobalConfig buildSlaveConfig() {
        GlobalConfig config = new GlobalConfig();
        config.setTenantId("slave-tenant");
        config.setWorkAsMaster(false);
        config.setMasterAddr("http://master:8080");
        config.setNetworkFullName("SlaveNet");
        config.setNetworkShortName("SN");
        config.setNetworkMode(GlobalConfig.NetworkMode.ONLY_5G);
        return config;
    }

    @Test
    @DisplayName("registerWithMaster - when masterAddr set, calls master endpoint")
    void registerWithMaster_whenMasterAddrSet_callsMasterEndpoint() {
        when(globalConfigRepository.findAll()).thenReturn(List.of(buildSlaveConfig()));

        slaveClientService.registerWithMaster();

        verify(restTemplate).exchange(
                eq("http://master:8080/api/v1/master/slaves/register"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    @DisplayName("registerWithMaster - when masterAddr empty, does nothing")
    void registerWithMaster_whenMasterAddrEmpty_doesNothing() {
        GlobalConfig config = buildSlaveConfig();
        config.setMasterAddr("");
        when(globalConfigRepository.findAll()).thenReturn(List.of(config));

        slaveClientService.registerWithMaster();

        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), any(Class.class));
    }

    @Test
    @DisplayName("registerWithMaster - when master call fails, does not propagate exception")
    void registerWithMaster_whenMasterCallFails_doesNotPropagateException() {
        when(globalConfigRepository.findAll()).thenReturn(List.of(buildSlaveConfig()));
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(Void.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThatCode(() -> slaveClientService.registerWithMaster()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendHeartbeat - when masterAddr set, calls heartbeat endpoint")
    void sendHeartbeat_whenMasterAddrSet_callsHeartbeatEndpoint() {
        when(globalConfigRepository.findAll()).thenReturn(List.of(buildSlaveConfig()));

        slaveClientService.sendHeartbeat();

        verify(restTemplate).exchange(
                eq("http://master:8080/api/v1/master/slaves/heartbeat"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    @DisplayName("applyConfigFromMaster - updates global config")
    void applyConfigFromMaster_updatesGlobalConfig() {
        GlobalConfig existing = buildSlaveConfig();
        when(networkConfigService.getGlobalConfig("slave-tenant")).thenReturn(existing);

        MultiTenantConfigPayload payload = new MultiTenantConfigPayload();
        payload.setNetworkFullName("MasterNet");
        payload.setNetworkShortName("MN");
        payload.setNetworkMode(GlobalConfig.NetworkMode.HYBRID_4G_5G);

        slaveClientService.applyConfigFromMaster("slave-tenant", payload);

        assertThat(existing.getNetworkFullName()).isEqualTo("MasterNet");
        assertThat(existing.getNetworkShortName()).isEqualTo("MN");
        assertThat(existing.getNetworkMode()).isEqualTo(GlobalConfig.NetworkMode.HYBRID_4G_5G);
        verify(globalConfigRepository).save(existing);
    }
}
