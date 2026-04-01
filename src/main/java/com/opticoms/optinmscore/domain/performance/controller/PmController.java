package com.opticoms.optinmscore.domain.performance.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.service.PmService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
public class PmController {

    private final PmService pmService;

    @Operation(summary = "Ingest a performance metric data point")
    @PostMapping("/metrics")
    public ResponseEntity<PmMetric> ingestMetric(
            HttpServletRequest request,
            @Valid @RequestBody PmMetric metric) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pmService.ingestMetric(tenantId, metric));
    }

    @Operation(summary = "Get metric history for a given time window")
    @GetMapping("/history")
    public ResponseEntity<List<PmMetric>> getMetricHistory(
            HttpServletRequest request,
            @RequestParam String metric,
            @RequestParam(defaultValue = "60") int minutes) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(pmService.getMetricsHistory(tenantId, metric, minutes));
    }

    @Operation(summary = "Get the latest value of a metric")
    @GetMapping("/current")
    public ResponseEntity<Double> getCurrentValue(
            HttpServletRequest request,
            @RequestParam String metric) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(pmService.getCurrentValue(tenantId, metric));
    }

    @Operation(summary = "Get total data transferred in GB over a time window")
    @GetMapping("/total-data")
    public ResponseEntity<Double> getTotalDataGB(
            HttpServletRequest request,
            @RequestParam(defaultValue = "60") int minutes) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(pmService.getTotalDataGB(tenantId, minutes));
    }

    @Operation(summary = "Get current uplink/downlink throughput in bps")
    @GetMapping("/throughput")
    public ResponseEntity<PmService.ThroughputResult> getCurrentThroughput(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(pmService.getCurrentThroughput(tenantId));
    }

    @Operation(summary = "Get time-bucketed traffic series for chart rendering (rx/tx byte deltas per bucket)")
    @GetMapping("/traffic-series")
    public ResponseEntity<PmService.TrafficSeriesResponse> getTrafficSeries(
            HttpServletRequest request,
            @RequestParam(defaultValue = "24h") String range) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(pmService.getTrafficSeries(tenantId, range));
    }

    @Operation(summary = "Get estimated traffic for a specific gNB")
    @GetMapping("/gnb-traffic")
    public ResponseEntity<PmService.GnbTrafficResult> getGnbTraffic(
            HttpServletRequest request,
            @RequestParam String gnbId,
            @RequestParam(defaultValue = "60") int minutes) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(pmService.getGnbTraffic(tenantId, gnbId, minutes));
    }
}
