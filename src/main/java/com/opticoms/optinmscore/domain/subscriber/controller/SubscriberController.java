package com.opticoms.optinmscore.domain.subscriber.controller;

import com.opticoms.optinmscore.domain.subscriber.dto.SubscriberRequest;
import com.opticoms.optinmscore.domain.subscriber.dto.SubscriberResponse;
import com.opticoms.optinmscore.domain.subscriber.importer.SubscriberImportParser;
import com.opticoms.optinmscore.domain.subscriber.mapper.SubscriberMapper;
import com.opticoms.optinmscore.domain.subscriber.model.BulkImportResult;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.subscriber.service.BulkImportService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscribers")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberService subscriberService;
    private final BulkImportService bulkImportService;
    private final List<SubscriberImportParser> parsers;
    private final SubscriberMapper subscriberMapper;

    @Operation(summary = "Bulk import subscribers from Excel (.xlsx), JSON, or CSV file")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResult> importSubscribers(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String tenantId = TenantContext.getCurrentTenantId(request);
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();

        SubscriberImportParser parser = parsers.stream()
                .filter(p -> p.supports(contentType, filename))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported file format. Use .xlsx, .json, or .csv"));

        try {
            List<Subscriber> parsed = parser.parse(file.getInputStream());

            if (parsed.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "File contains no subscriber records");
            }

            BulkImportResult result = bulkImportService.bulkImport(tenantId, parsed);
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to parse file: " + e.getMessage());
        }
    }

    @Operation(summary = "Create a new subscriber and provision to Open5GS")
    @PostMapping
    public ResponseEntity<SubscriberResponse> createSubscriber(
            HttpServletRequest request,
            @Valid @RequestBody SubscriberRequest subscriberRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Subscriber entity = subscriberMapper.toEntity(subscriberRequest);
        Subscriber saved = subscriberService.createSubscriber(tenantId, entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriberMapper.toResponse(saved));
    }

    @Operation(summary = "Get subscriber by IMSI")
    @GetMapping("/{imsi}")
    public ResponseEntity<SubscriberResponse> getSubscriber(
            HttpServletRequest request,
            @PathVariable String imsi) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(subscriberMapper.toResponse(
                subscriberService.getSubscriber(tenantId, imsi)));
    }

    @Operation(summary = "Update subscriber by IMSI")
    @PutMapping("/{imsi}")
    public ResponseEntity<SubscriberResponse> updateSubscriber(
            HttpServletRequest request,
            @PathVariable String imsi,
            @Valid @RequestBody SubscriberRequest subscriberRequest) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Subscriber entity = subscriberMapper.toEntity(subscriberRequest);
        return ResponseEntity.ok(subscriberMapper.toResponse(
                subscriberService.updateSubscriber(tenantId, imsi, entity)));
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
    public ResponseEntity<Page<SubscriberResponse>> listSubscribers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return ResponseEntity.ok(subscriberService.getAllSubscribersPaged(tenantId, pageable)
                .map(subscriberMapper::toResponse));
    }

    @Operation(summary = "Search subscribers by label (text), IMSI (15 digits exact), or MSISDN (10-15 digits exact)")
    @GetMapping("/search")
    public ResponseEntity<Page<SubscriberResponse>> searchSubscribers(
            HttpServletRequest request,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(subscriberService.searchSubscribers(tenantId, q, pageable)
                .map(subscriberMapper::toResponse));
    }

    @Operation(summary = "Get total subscriber count")
    @GetMapping("/count")
    public ResponseEntity<Long> getSubscriberCount(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        return ResponseEntity.ok(subscriberService.getSubscriberCount(tenantId));
    }
}