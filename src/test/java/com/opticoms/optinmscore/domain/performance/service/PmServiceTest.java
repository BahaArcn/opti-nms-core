package com.opticoms.optinmscore.domain.performance.service;

import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.repository.PmMetricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PmServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private PmMetricRepository pmMetricRepository;

    @InjectMocks
    private PmService service;

    @Test
    void ingestMetric_setsTenantId() {
        PmMetric metric = buildMetric("cpu_usage", 85.5);
        when(pmMetricRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PmMetric result = service.ingestMetric(TENANT, metric);

        assertEquals(TENANT, result.getTenantId());
    }

    @Test
    void ingestMetric_nullTimestamp_setsCurrentTime() {
        PmMetric metric = buildMetric("cpu_usage", 85.5);
        metric.setTimestamp(null);
        when(pmMetricRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PmMetric result = service.ingestMetric(TENANT, metric);

        assertNotNull(result.getTimestamp());
    }

    @Test
    void ingestMetric_existingTimestamp_preserved() {
        PmMetric metric = buildMetric("cpu_usage", 85.5);
        long customTs = 1700000000000L;
        metric.setTimestamp(customTs);
        when(pmMetricRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PmMetric result = service.ingestMetric(TENANT, metric);

        assertEquals(customTs, result.getTimestamp());
    }

    @Test
    void getMetricsHistory_delegatesToRepo() {
        List<PmMetric> expected = List.of(buildMetric("cpu_usage", 85.5));
        when(pmMetricRepository.findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                eq(TENANT), eq("cpu_usage"), anyLong(), anyLong()))
                .thenReturn(expected);

        List<PmMetric> result = service.getMetricsHistory(TENANT, "cpu_usage", 60);

        assertEquals(expected, result);
    }

    @Test
    void getMetricsHistory_calculatesTimeWindow() {
        when(pmMetricRepository.findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                eq(TENANT), eq("cpu_usage"), anyLong(), anyLong()))
                .thenReturn(List.of());

        service.getMetricsHistory(TENANT, "cpu_usage", 30);

        ArgumentCaptor<Long> startCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> endCaptor = ArgumentCaptor.forClass(Long.class);
        verify(pmMetricRepository).findByTenantIdAndMetricNameAndTimestampBetweenOrderByTimestampDesc(
                eq(TENANT), eq("cpu_usage"), startCaptor.capture(), endCaptor.capture());

        long diff = endCaptor.getValue() - startCaptor.getValue();
        assertEquals(30 * 60 * 1000, diff, 1000);
    }

    @Test
    void getCurrentValue_metricExists_returnsValue() {
        PmMetric metric = buildMetric("cpu_usage", 92.3);
        when(pmMetricRepository.findFirstByTenantIdAndMetricNameOrderByTimestampDesc(TENANT, "cpu_usage"))
                .thenReturn(metric);

        Double result = service.getCurrentValue(TENANT, "cpu_usage");

        assertEquals(92.3, result);
    }

    @Test
    void getCurrentValue_noMetric_returnsZero() {
        when(pmMetricRepository.findFirstByTenantIdAndMetricNameOrderByTimestampDesc(TENANT, "cpu_usage"))
                .thenReturn(null);

        Double result = service.getCurrentValue(TENANT, "cpu_usage");

        assertEquals(0.0, result);
    }

    private PmMetric buildMetric(String name, double value) {
        PmMetric m = new PmMetric();
        m.setMetricName(name);
        m.setValue(value);
        return m;
    }
}
