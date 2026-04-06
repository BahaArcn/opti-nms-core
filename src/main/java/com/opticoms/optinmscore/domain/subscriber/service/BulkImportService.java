package com.opticoms.optinmscore.domain.subscriber.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import com.opticoms.optinmscore.domain.subscriber.model.BulkImportResult;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.subscriber.repository.SubscriberRepository;
import com.opticoms.optinmscore.integration.open5gs.Open5gsProvisioningService;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkImportService {

    private final SubscriberRepository subscriberRepository;
    private final EncryptionService encryptionService;
    private final Open5gsProvisioningService open5gsProvisioning;
    private final LicenseService licenseService;
    private final SubscriberHelper subscriberHelper;

    @Audited(action = AuditAction.CREATE, entityType = "Subscriber")
    public BulkImportResult bulkImport(String tenantId, List<Subscriber> parsed) {
        BulkImportResult.BulkImportResultBuilder result = BulkImportResult.builder();
        List<BulkImportResult.RowError> errors = new ArrayList<>();
        result.totalInFile(parsed.size());

        // Phase 1: License quota check
        int remaining = licenseService.getRemainingSubscriberQuota(tenantId);
        if (remaining == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "License is not active or subscriber quota is exhausted");
        }

        // Phase 2: Validate ALL records
        List<Subscriber> validSubscribers = new ArrayList<>();
        int failedValidation = 0;

        for (int i = 0; i < parsed.size(); i++) {
            Subscriber sub = parsed.get(i);
            int row = i + 1;
            try {
                if (sub.getImsi() == null || !sub.getImsi().matches("^\\d{15}$")) {
                    errors.add(new BulkImportResult.RowError(row, sub.getImsi(), "imsi",
                            "IMSI must be exactly 15 digits"));
                    failedValidation++;
                    continue;
                }
                if (sub.getUsimType() == null) {
                    errors.add(new BulkImportResult.RowError(row, sub.getImsi(), "usimType",
                            "usimType is required (OP or OPC)"));
                    failedValidation++;
                    continue;
                }
                if (sub.getProfileList() == null || sub.getProfileList().isEmpty()) {
                    errors.add(new BulkImportResult.RowError(row, sub.getImsi(), "profileList",
                            "At least one profile (DNN) is required"));
                    failedValidation++;
                    continue;
                }
                boolean hasValidDnn = sub.getProfileList().stream()
                        .anyMatch(p -> p.getApnDnn() != null && !p.getApnDnn().isBlank());
                if (!hasValidDnn) {
                    errors.add(new BulkImportResult.RowError(row, sub.getImsi(), "dnn",
                            "At least one profile must have a DNN/APN name"));
                    failedValidation++;
                    continue;
                }
                subscriberHelper.validateKeys(sub);
                validSubscribers.add(sub);
            } catch (ResponseStatusException e) {
                errors.add(new BulkImportResult.RowError(row, sub.getImsi(), "validation",
                        e.getReason()));
                failedValidation++;
            }
        }

        // Phase 3: In-file duplicate IMSI detection
        Set<String> seenImsis = new LinkedHashSet<>();
        List<Subscriber> uniqueSubscribers = new ArrayList<>();
        int skippedDuplicateInFile = 0;

        for (Subscriber sub : validSubscribers) {
            if (!seenImsis.add(sub.getImsi())) {
                skippedDuplicateInFile++;
            } else {
                uniqueSubscribers.add(sub);
            }
        }

        // Phase 4: DB duplicate check via single $in query
        Map<String, String> imsiToHash = new HashMap<>();
        for (Subscriber sub : uniqueSubscribers) {
            imsiToHash.put(sub.getImsi(), encryptionService.hash(sub.getImsi()));
        }

        Set<String> existingHashes = subscriberRepository
                .findByTenantIdAndImsiHashIn(tenantId, imsiToHash.values())
                .stream()
                .map(Subscriber::getImsiHash)
                .collect(Collectors.toSet());

        List<Subscriber> newSubscribers = new ArrayList<>();
        int skippedDuplicateInDb = 0;

        for (Subscriber sub : uniqueSubscribers) {
            if (existingHashes.contains(imsiToHash.get(sub.getImsi()))) {
                skippedDuplicateInDb++;
            } else {
                newSubscribers.add(sub);
            }
        }

        // Phase 5: Trim to license quota
        int skippedDueToLicense = 0;
        List<Subscriber> toImport;

        if (remaining != Integer.MAX_VALUE && newSubscribers.size() > remaining) {
            toImport = new ArrayList<>(newSubscribers.subList(0, remaining));
            skippedDueToLicense = newSubscribers.size() - remaining;
        } else {
            toImport = newSubscribers;
        }

        if (toImport.isEmpty()) {
            return result
                    .successCount(0)
                    .failedValidation(failedValidation)
                    .skippedDueToLicense(skippedDueToLicense)
                    .skippedDuplicateInFile(skippedDuplicateInFile)
                    .skippedDuplicateInDb(skippedDuplicateInDb)
                    .errors(errors)
                    .message("No new subscribers to import")
                    .build();
        }

        // Phase 6: DNN/APN enrichment
        for (Subscriber sub : toImport) {
            sub.setTenantId(tenantId);
            subscriberHelper.enrichProfilesFromApn(tenantId, sub);
        }

        // Phase 7: Open5GS bulk provisioning (before encryption — Open5GS needs plaintext)
        String open5gsUri = subscriberHelper.resolveOpen5gsUri(tenantId);
        var provisionResult = open5gsProvisioning.provisionSubscribersBulk(toImport, open5gsUri);

        Set<String> failedOpen5gs = new HashSet<>(provisionResult.getFailedImsis());
        List<Subscriber> provisionedSubscribers;
        if (failedOpen5gs.isEmpty()) {
            provisionedSubscribers = toImport;
        } else {
            provisionedSubscribers = toImport.stream()
                    .filter(sub -> !failedOpen5gs.contains(sub.getImsi()))
                    .collect(Collectors.toList());
            for (String failedImsi : provisionResult.getFailedImsis()) {
                errors.add(new BulkImportResult.RowError(0, failedImsi,
                        "open5gs", "Failed to provision to Open5GS"));
            }
        }

        // Phase 8: Encrypt + batch save to NMS MongoDB
        for (Subscriber sub : provisionedSubscribers) {
            subscriberHelper.encryptSensitiveData(sub);
        }

        List<Subscriber> saved = subscriberRepository.saveAll(provisionedSubscribers);
        int successCount = saved.size();

        log.info("Bulk import for tenant={}: total={}, success={}, failedValidation={}, " +
                        "dupInFile={}, dupInDb={}, licenseTrim={}, open5gsFail={}",
                tenantId, parsed.size(), successCount, failedValidation,
                skippedDuplicateInFile, skippedDuplicateInDb, skippedDueToLicense,
                failedOpen5gs.size());

        String message;
        if (skippedDueToLicense > 0) {
            message = String.format(
                    "License limit reached. %d of %d subscribers imported. %d skipped due to license limit.",
                    successCount, parsed.size(), skippedDueToLicense);
        } else if (successCount == parsed.size()) {
            message = String.format("All %d subscribers imported successfully.", successCount);
        } else {
            message = String.format("%d of %d subscribers imported. Check errors for details.",
                    successCount, parsed.size());
        }

        return result
                .successCount(successCount)
                .failedValidation(failedValidation)
                .skippedDueToLicense(skippedDueToLicense)
                .skippedDuplicateInFile(skippedDuplicateInFile)
                .skippedDuplicateInDb(skippedDuplicateInDb)
                .errors(errors)
                .message(message)
                .build();
    }
}
