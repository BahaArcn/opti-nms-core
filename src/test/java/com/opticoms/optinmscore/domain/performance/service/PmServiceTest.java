package com.opticoms.optinmscore.domain.performance.service;

import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.repository.PmMetricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PmServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private PmMetricRepository pmMetricRepository;
    @Mock private ConnectedUeRepository connectedUeRepository;
    @Mock private MongoTemplate mongoTemplate;
    @InjectMocks private PmService pmService;

    @Test
    void getCurrentValue_returnsLatestMetric() {
        PmMetric metric = new PmMetric();
        metric.setValue(42.5);
        when(pmMetricRepository.findFirstByTenantIdAndMetricNameOrderByTimestampDesc(TENANT, "upf_rx_bytes"))
                .thenReturn(metric);

        Double result = pmService.getCurrentValue(TENANT, "upf_rx_bytes");

        assertEquals(42.5, result);
    }

    @Test
    void getCurrentValue_noData_returnsZero() {
        when(pmMetricRepository.findFirstByTenantIdAndMetricNameOrderByTimestampDesc(TENANT, "upf_rx_bytes"))
                .thenReturn(null);

        Double result = pmService.getCurrentValue(TENANT, "upf_rx_bytes");

        assertEquals(0.0, result);
    }

    @Test
    void ingestMetric_setsTimestampIfMissing() {
        PmMetric metric = new PmMetric();
        metric.setMetricName("test_metric");
        metric.setValue(100.0);
        when(pmMetricRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PmMetric result = pmService.ingestMetric(TENANT, metric);

        assertEquals(TENANT, result.getTenantId());
        assertNotNull(result.getTimestamp());
        assertNotNull(result.getExpiresAt());
    }

    @Test
    void ingestMetric_delegatesToRepository() {
        PmMetric metric = new PmMetric();
        metric.setMetricName("test_metric");
        metric.setValue(50.0);
        metric.setTimestamp(System.currentTimeMillis());
        when(pmMetricRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pmService.ingestMetric(TENANT, metric);

        verify(pmMetricRepository).save(any(PmMetric.class));
    }
}
