package com.opticoms.optinmscore.domain.subscriber.controller;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.subscriber.service.SubscriberService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.opticoms.optinmscore.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriber")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;

    @Operation(summary = "Create a new subscriber and provision to Open5GS")
    @PostMapping
    public ResponseEntity<Subscriber> createSubscriber(
            HttpServletRequest request,
            @Valid @RequestBody Subscriber subscriber) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriberService.createSubscriber(tenantId, subscriber));
    }

    @Operation(summary = "Get subscriber by IMSI")
    @GetMapping("/{imsi}")
    public ResponseEntity<Subscriber> getSubscriber(
            HttpServletRequest request,
            @PathVariable String imsi) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(subscriberService.getSubscriber(tenantId, imsi));
    }

    @Operation(summary = "Update subscriber by IMSI")
    @PutMapping("/{imsi}")
    public ResponseEntity<Subscriber> updateSubscriber(
            HttpServletRequest request,
            @PathVariable String imsi,
            @Valid @RequestBody Subscriber subscriber) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(subscriberService.updateSubscriber(tenantId, imsi, subscriber));
    }

    @Operation(summary = "Delete subscriber by IMSI")
    @DeleteMapping("/{imsi}")
    public ResponseEntity<Void> deleteSubscriber(
            HttpServletRequest request,
            @PathVariable String imsi) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        subscriberService.deleteSubscriber(tenantId, imsi);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Batch delete subscribers by IMSI list")
    @DeleteMapping("/batch")
    public ResponseEntity<Void> deleteSubscribersBatch(
            HttpServletRequest request,
            @RequestBody List<String> imsiList) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        subscriberService.deleteSubscribersBatch(tenantId, imsiList);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List subscribers with pagination (use GET /{imsi} for exact IMSI lookup)")
    @GetMapping("/list")
    public ResponseEntity<Page<Subscriber>> listSubscribers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(subscriberService.getAllSubscribersPaged(tenantId, pageable));
    }

    @Operation(summary = "Search subscribers by label (text), IMSI (15 digits exact), or MSISDN (10-15 digits exact)")
    @GetMapping("/search")
    public ResponseEntity<Page<Subscriber>> searchSubscribers(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(subscriberService.searchSubscribers(tenantId, q, pageable));
    }

    @Operation(summary = "Get total subscriber count")
    @GetMapping("/count")
    public ResponseEntity<Long> getSubscriberCount(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(subscriberService.getSubscriberCount(tenantId));
    }
}