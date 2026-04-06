package com.opticoms.optinmscore.domain.performance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.performance.dto.PmMetricRequest;
import com.opticoms.optinmscore.domain.performance.dto.PmMetricResponse;
import com.opticoms.optinmscore.domain.performance.mapper.PmMetricMapper;
import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.service.PmService;
import com.opticoms.optinmscore.security.JwtService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PmController.class)
@AutoConfigureMockMvc(addFilters = false)
class PmControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PmService pmService;
    @MockBean private PmMetricMapper pmMetricMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void stubPmMetricMapper() {
        when(pmMetricMapper.toEntity(any(PmMetricRequest.class))).thenAnswer(invocation -> {
            PmMetricRequest req = invocation.getArgument(0);
            PmMetric m = new PmMetric();
            m.setMetricName(req.getMetricName());
            m.setValue(req.getValue());
            m.setTimestamp(req.getTimestamp());
            m.setLabels(req.getLabels());
            m.setMetricType(req.getMetricType());
            return m;
        });
        when(pmMetricMapper.toResponse(any(PmMetric.class))).thenAnswer(invocation -> {
            PmMetric e = invocation.getArgument(0);
            PmMetricResponse r = new PmMetricResponse();
            r.setMetricName(e.getMetricName());
            r.setValue(e.getValue());
            return r;
        });
        when(pmMetricMapper.toResponseList(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<PmMetric> list = invocation.getArgument(0);
            return list.stream()
                    .map(e -> {
                        PmMetricResponse r = new PmMetricResponse();
                        r.setMetricName(e.getMetricName());
                        r.setValue(e.getValue());
                        return r;
                    })
                    .toList();
        });
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void ingestMetric_returns201() throws Exception {
        PmMetric metric = new PmMetric();
        metric.setMetricName("cpu_usage");
        metric.setValue(85.5);

        PmMetricRequest request = new PmMetricRequest();
        request.setMetricName("cpu_usage");
        request.setValue(85.5);

        when(pmService.ingestMetric(eq(TENANT), any())).thenReturn(metric);

        mockMvc.perform(post("/api/v1/performance/metrics")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metricName").value("cpu_usage"))
                .andExpect(jsonPath("$.value").value(85.5));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getMetricHistory_returns200() throws Exception {
        PmMetric metric = new PmMetric();
        metric.setMetricName("cpu_usage");
        metric.setValue(85.5);

        when(pmService.getMetricsHistory(TENANT, "cpu_usage", 60))
                .thenReturn(List.of(metric));

        mockMvc.perform(get("/api/v1/performance/history")
                        .requestAttr("tenantId", TENANT)
                        .param("metric", "cpu_usage")
                        .param("minutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metricName").value("cpu_usage"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void getCurrentValue_returns200() throws Exception {
        when(pmService.getCurrentValue(TENANT, "cpu_usage")).thenReturn(92.3);

        mockMvc.perform(get("/api/v1/performance/current")
                        .requestAttr("tenantId", TENANT)
                        .param("metric", "cpu_usage"))
                .andExpect(status().isOk())
                .andExpect(content().string("92.3"));
    }
}
