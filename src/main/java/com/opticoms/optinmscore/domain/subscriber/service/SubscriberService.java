package com.opticoms.optinmscore.domain.subscriber.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.subscriber.repository.SubscriberRepository;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final EncryptionService encryptionService;
    private final SubscriberSyncService subscriberSync;
    private final ConnectedUeRepository connectedUeRepository;
    private final LicenseService licenseService;
    private final SubscriberHelper subscriberHelper;

    @Audited(action = AuditAction.CREATE, entityType = "Subscriber")
    public Subscriber createSubscriber(String tenantId, Subscriber subscriber) {
        licenseService.checkCanAddSubscriber(tenantId);

        String imsiHash = encryptionService.hash(subscriber.getImsi());

        if (subscriberRepository.existsByTenantIdAndImsiHash(tenantId, imsiHash)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Subscriber with the given IMSI already exists");
        }

        subscriberHelper.validateKeys(subscriber);
        subscriberHelper.validatePolicyReference(tenantId, subscriber);
        subscriberHelper.enrichProfilesFromApn(tenantId, subscriber);
        subscriber.setTenantId(tenantId);

        String open5gsUri = subscriberHelper.resolveOpen5gsUri(tenantId);
        String plaintextImsi = subscriber.getImsi();
        subscriberSync.provision(subscriber, open5gsUri);

        try {
            subscriberHelper.encryptSensitiveData(subscriber);
            return subscriberRepository.save(subscriber);
        } catch (Exception e) {
            subscriberSync.rollbackProvision(plaintextImsi, open5gsUri);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to persist subscriber after provisioning");
        }
    }

    public Subscriber getSubscriber(String tenantId, String imsi) {
        String imsiHash = encryptionService.hash(imsi);
        Subscriber sub = subscriberRepository.findByImsiHashAndTenantId(imsiHash, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subscriber not found"));

        subscriberHelper.decryptSensitiveData(sub);
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

        subscriberHelper.validateKeys(updatedData);
        subscriberHelper.validatePolicyReference(tenantId, updatedData);
        subscriberHelper.enrichProfilesFromApn(tenantId, updatedData);

        String open5gsUri = subscriberHelper.resolveOpen5gsUri(tenantId);
        subscriberSync.provision(updatedData, open5gsUri);

        try {
            subscriberHelper.encryptSensitiveData(updatedData);
            return subscriberRepository.save(updatedData);
        } catch (Exception e) {
            subscriberSync.rollbackUpdate(existing, open5gsUri);
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

        String open5gsUri = subscriberHelper.resolveOpen5gsUri(tenantId);
        subscriberSync.deleteQuietly(imsi, open5gsUri);
        subscriberRepository.delete(sub);
    }

    @Audited(action = AuditAction.DELETE, entityType = "Subscriber")
    public int deleteSubscribersBatch(String tenantId, List<String> imsiList) {
        String open5gsUri = subscriberHelper.resolveOpen5gsUri(tenantId);

        List<String> hashes = imsiList.stream()
                .map(encryptionService::hash).toList();
        List<Subscriber> found = subscriberRepository
                .findByTenantIdAndImsiHashIn(tenantId, hashes);
        if (found.isEmpty()) return 0;

        List<String> plainImsis = found.stream()
                .map(s -> encryptionService.decrypt(s.getImsi()))
                .toList();
        subscriberSync.deleteBulkQuietly(plainImsis, open5gsUri);

        subscriberRepository.deleteAll(found);
        return found.size();
    }

    public Page<Subscriber> getAllSubscribersPaged(String tenantId, Pageable pageable) {
        Page<Subscriber> page = subscriberRepository.findByTenantId(tenantId, pageable);
        page.getContent().forEach(subscriberHelper::decryptSensitiveData);
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
                subscriberHelper.decryptSensitiveData(sub);
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
                subscriberHelper.decryptSensitiveData(sub);
                enrichWithConnectionStatus(tenantId, List.of(sub));
                return new org.springframework.data.domain.PageImpl<>(List.of(sub), pageable, 1);
            }
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
        }

        Page<Subscriber> page = subscriberRepository.findByTenantIdAndLabelContainingIgnoreCase(
                tenantId, trimmed, pageable);
        page.getContent().forEach(subscriberHelper::decryptSensitiveData);
        enrichWithConnectionStatus(tenantId, page.getContent());
        return page;
    }

    public long getSubscriberCount(String tenantId) {
        return subscriberRepository.countByTenantId(tenantId);
    }

    private void enrichWithConnectionStatus(String tenantId, List<Subscriber> subscribers) {
        if (subscribers.isEmpty()) return;

        List<String> pageImsis = subscribers.stream()
                .map(Subscriber::getImsi)
                .filter(Objects::nonNull)
                .toList();

        Map<String, ConnectedUe> ueMap = connectedUeRepository
                .findByTenantIdAndImsiIn(tenantId, pageImsis)
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

}
