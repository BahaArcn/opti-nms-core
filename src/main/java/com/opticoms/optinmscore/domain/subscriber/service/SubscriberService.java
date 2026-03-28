package com.opticoms.optinmscore.domain.subscriber.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import com.opticoms.optinmscore.domain.policy.service.PolicyService;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.subscriber.repository.SubscriberRepository;
import com.opticoms.optinmscore.domain.tenant.repository.TenantRepository;
import com.opticoms.optinmscore.integration.open5gs.Open5gsProvisioningService;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final EncryptionService encryptionService;
    private final Open5gsProvisioningService open5gsProvisioning;
    private final TenantRepository tenantRepository;
    private final ConnectedUeRepository connectedUeRepository;
    private final PolicyService policyService;
    private final LicenseService licenseService;

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]+$");

    @Audited(action = AuditAction.CREATE, entityType = "Subscriber")
    public Subscriber createSubscriber(String tenantId, Subscriber subscriber) {
        licenseService.checkCanAddSubscriber(tenantId);

        String imsiHash = encryptionService.hash(subscriber.getImsi());

        if (subscriberRepository.existsByTenantIdAndImsiHash(tenantId, imsiHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Subscriber with the given IMSI already exists");
        }

        validateKeys(subscriber);
        validatePolicyReference(tenantId, subscriber);
        subscriber.setTenantId(tenantId);

        String open5gsUri = resolveOpen5gsUri(tenantId);
        String plaintextImsi = subscriber.getImsi();
        provisionToOpen5gs(subscriber, open5gsUri);

        try {
            encryptSensitiveData(subscriber);
            return subscriberRepository.save(subscriber);
        } catch (Exception e) {
            if (open5gsUri != null) {
                try {
                    open5gsProvisioning.deleteSubscriber(plaintextImsi, open5gsUri);
                    log.warn("NMS save failed for subscriber, rolled back Open5GS provisioning");
                } catch (Exception rollbackEx) {
                    log.error("CRITICAL: Subscriber is provisioned in Open5GS but " +
                            "failed to save to NMS. Manual cleanup required.", rollbackEx);
                }
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to persist subscriber after provisioning");
        }
    }

    public Subscriber getSubscriber(String tenantId, String imsi) {
        String imsiHash = encryptionService.hash(imsi);
        Subscriber sub = subscriberRepository.findByImsiHashAndTenantId(imsiHash, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subscriber not found"));

        decryptSensitiveData(sub);
        return sub;
    }

    @Audited(action = AuditAction.UPDATE, entityType = "Subscriber")
    public Subscriber updateSubscriber(String tenantId, String imsi, Subscriber updatedData) {
        Subscriber existing = getSubscriber(tenantId, imsi);

        updatedData.setId(existing.getId());
        updatedData.setVersion(existing.getVersion());
        updatedData.setCreatedAt(existing.getCreatedAt());
        updatedData.setCreatedBy(existing.getCreatedBy());
        updatedData.setTenantId(tenantId);

        validateKeys(updatedData);
        validatePolicyReference(tenantId, updatedData);

        String open5gsUri = resolveOpen5gsUri(tenantId);
        provisionToOpen5gs(updatedData, open5gsUri);

        try {
            encryptSensitiveData(updatedData);
            return subscriberRepository.save(updatedData);
        } catch (Exception e) {
            if (open5gsUri != null) {
                try {
                    open5gsProvisioning.provisionSubscriber(existing, open5gsUri);
                    log.warn("NMS update failed for subscriber, reverted Open5GS to previous state");
                } catch (Exception rollbackEx) {
                    log.error("CRITICAL: Open5GS has new data for subscriber but NMS " +
                            "has old data. Manual sync required.", rollbackEx);
                }
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to update subscriber in NMS");
        }
    }

    @Audited(action = AuditAction.DELETE, entityType = "Subscriber")
    public void deleteSubscriber(String tenantId, String imsi) {
        String imsiHash = encryptionService.hash(imsi);
        Subscriber sub = subscriberRepository.findByImsiHashAndTenantId(imsiHash, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subscriber not found"));

        String open5gsUri = resolveOpen5gsUri(tenantId);
        try {
            open5gsProvisioning.deleteSubscriber(imsi, open5gsUri);
        } catch (Exception e) {
            log.warn("Failed to delete subscriber from Open5GS (will still remove from local DB): {}",
                    e.getMessage());
        }

        subscriberRepository.delete(sub);
    }

    @Audited(action = AuditAction.DELETE, entityType = "Subscriber")
    public int deleteSubscribersBatch(String tenantId, List<String> imsiList) {
        String open5gsUri = resolveOpen5gsUri(tenantId);
        int deleted = 0;
        for (String imsi : imsiList) {
            String imsiHash = encryptionService.hash(imsi);
            var found = subscriberRepository.findByImsiHashAndTenantId(imsiHash, tenantId);
            if (found.isPresent()) {
                try {
                    open5gsProvisioning.deleteSubscriber(imsi, open5gsUri);
                } catch (Exception e) {
                    log.warn("Failed to delete subscriber from Open5GS: {}", e.getMessage());
                }
                subscriberRepository.delete(found.get());
                deleted++;
            }
        }
        return deleted;
    }

    public List<Subscriber> getAllSubscribers(String tenantId) {
        List<Subscriber> subscribers = subscriberRepository.findByTenantId(tenantId);
        subscribers.forEach(this::decryptSensitiveData);
        return subscribers;
    }

    public Page<Subscriber> getAllSubscribersPaged(String tenantId, Pageable pageable) {
        Page<Subscriber> page = subscriberRepository.findByTenantId(tenantId, pageable);
        page.getContent().forEach(this::decryptSensitiveData);
        enrichWithConnectionStatus(tenantId, page.getContent());
        return page;
    }

    public Page<Subscriber> searchSubscribers(String tenantId, String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return getAllSubscribersPaged(tenantId, pageable);
        }

        String trimmed = query.trim();

        if (trimmed.matches("^\\d{15}$")) {
            String imsiHash = encryptionService.hash(trimmed);
            Optional<Subscriber> found = subscriberRepository.findByImsiHashAndTenantId(imsiHash, tenantId);
            if (found.isPresent()) {
                Subscriber sub = found.get();
                decryptSensitiveData(sub);
                enrichWithConnectionStatus(tenantId, List.of(sub));
                return new org.springframework.data.domain.PageImpl<>(List.of(sub), pageable, 1);
            }
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
        }

        if (trimmed.matches("^\\d{10,15}$")) {
            String msisdnHash = encryptionService.hash(trimmed);
            Optional<Subscriber> found = subscriberRepository.findByMsisdnHashAndTenantId(msisdnHash, tenantId);
            if (found.isPresent()) {
                Subscriber sub = found.get();
                decryptSensitiveData(sub);
                enrichWithConnectionStatus(tenantId, List.of(sub));
                return new org.springframework.data.domain.PageImpl<>(List.of(sub), pageable, 1);
            }
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
        }

        Page<Subscriber> page = subscriberRepository.findByTenantIdAndLabelContainingIgnoreCase(
                tenantId, trimmed, pageable);
        page.getContent().forEach(this::decryptSensitiveData);
        enrichWithConnectionStatus(tenantId, page.getContent());
        return page;
    }

    public long getSubscriberCount(String tenantId) {
        return subscriberRepository.countByTenantId(tenantId);
    }

    private void enrichWithConnectionStatus(String tenantId, List<Subscriber> subscribers) {
        Map<String, ConnectedUe> ueMap = connectedUeRepository.findByTenantId(tenantId)
                .stream()
                .collect(Collectors.toMap(ConnectedUe::getImsi, Function.identity(), (a, b) -> a));

        for (Subscriber sub : subscribers) {
            ConnectedUe ue = ueMap.get(sub.getImsi());
            if (ue == null || ue.getStatus() == ConnectedUe.UeStatus.DISCONNECTED) {
                sub.setConnectionStatus(Subscriber.ConnectionStatus.DISCONNECTED);
            } else if (ue.getStatus() == ConnectedUe.UeStatus.CONNECTED) {
                sub.setConnectionStatus(Subscriber.ConnectionStatus.CONNECTED);
            } else {
                sub.setConnectionStatus(Subscriber.ConnectionStatus.UNKNOWN);
            }
        }
    }

    private String resolveOpen5gsUri(String tenantId) {
        return tenantRepository.findByTenantId(tenantId)
                .map(t -> t.getOpen5gsMongoUri())
                .orElse(null);
    }

    /**
     * Provisions the subscriber to Open5GS MongoDB with plaintext keys.
     * Open5GS needs raw Ki/OPc for authentication -- never encrypted values.
     */
    private void provisionToOpen5gs(Subscriber subscriber, String open5gsMongoUri) {
        try {
            open5gsProvisioning.provisionSubscriber(subscriber, open5gsMongoUri);
        } catch (Exception e) {
            log.error("Failed to provision subscriber to Open5GS: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to provision subscriber to Open5GS core: " + e.getMessage());
        }
    }

    private void validatePolicyReference(String tenantId, Subscriber sub) {
        if (sub.getPolicyId() != null && !sub.getPolicyId().isBlank()) {
            if (!policyService.existsForTenant(tenantId, sub.getPolicyId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Referenced policy not found: " + sub.getPolicyId());
            }
        }
    }

    private void validateKeys(Subscriber sub) {
        validateHex(sub.getKi(), 16, "Ki");
        if (sub.getUsimType() == Subscriber.UsimType.OPC) {
            validateHex(sub.getOpc(), 16, "OPc");
        } else if (sub.getUsimType() == Subscriber.UsimType.OP) {
            validateHex(sub.getOp(), 16, "OP");
        }
    }

    private void encryptSensitiveData(Subscriber sub) {
        sub.setImsiHash(encryptionService.hash(sub.getImsi()));
        sub.setImsi(encryptionService.encrypt(sub.getImsi()));

        if (sub.getMsisdn() != null && !sub.getMsisdn().isEmpty()) {
            sub.setMsisdnHash(encryptionService.hash(sub.getMsisdn()));
            sub.setMsisdn(encryptionService.encrypt(sub.getMsisdn()));
        }

        sub.setKi(encryptionService.encrypt(sub.getKi()));

        if (sub.getUsimType() == Subscriber.UsimType.OPC) {
            sub.setOpc(encryptionService.encrypt(sub.getOpc()));
            sub.setOp(null);
        } else if (sub.getUsimType() == Subscriber.UsimType.OP) {
            sub.setOp(encryptionService.encrypt(sub.getOp()));
            sub.setOpc(null);
        }
    }

    private void decryptSensitiveData(Subscriber sub) {
        sub.setImsi(encryptionService.decrypt(sub.getImsi()));
        if (sub.getMsisdn() != null && !sub.getMsisdn().isEmpty()) {
            sub.setMsisdn(encryptionService.decrypt(sub.getMsisdn()));
        }
        sub.setKi(encryptionService.decrypt(sub.getKi()));
        if (sub.getOpc() != null) sub.setOpc(encryptionService.decrypt(sub.getOpc()));
        if (sub.getOp() != null) sub.setOp(encryptionService.decrypt(sub.getOp()));
    }

    private void validateHex(String value, int requiredBytes, String fieldName) {
        if (value == null || value.length() != (requiredBytes * 2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must be exactly " + requiredBytes + " bytes (" + (requiredBytes * 2) + " hex chars)");
        }
        if (!HEX_PATTERN.matcher(value).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must contain only hexadecimal characters (0-9, A-F)");
        }
    }
}
