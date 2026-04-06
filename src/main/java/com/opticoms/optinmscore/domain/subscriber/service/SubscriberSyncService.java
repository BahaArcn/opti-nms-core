package com.opticoms.optinmscore.domain.subscriber.service;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.integration.open5gs.Open5gsProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriberSyncService {

    private final Open5gsProvisioningService open5gsProvisioning;

    public void provision(Subscriber subscriber, String open5gsUri) {
        if (open5gsUri == null) return;
        try {
            open5gsProvisioning.provisionSubscriber(subscriber, open5gsUri);
        } catch (Exception e) {
            log.error("Failed to provision subscriber to Open5GS: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to provision subscriber to Open5GS core: " + e.getMessage());
        }
    }

    public void rollbackProvision(String plainImsi, String open5gsUri) {
        if (open5gsUri == null) return;
        try {
            open5gsProvisioning.deleteSubscriber(plainImsi, open5gsUri);
            log.warn("NMS save failed for subscriber, rolled back Open5GS provisioning");
        } catch (Exception e) {
            log.error("CRITICAL: Subscriber is provisioned in Open5GS but " +
                    "failed to save to NMS. Manual cleanup required.", e);
        }
    }

    public void rollbackUpdate(Subscriber previous, String open5gsUri) {
        if (open5gsUri == null) return;
        try {
            open5gsProvisioning.provisionSubscriber(previous, open5gsUri);
            log.warn("NMS update failed for subscriber, reverted Open5GS to previous state");
        } catch (Exception e) {
            log.error("CRITICAL: Open5GS has new data for subscriber but NMS " +
                    "has old data. Manual sync required.", e);
        }
    }

    public void deleteQuietly(String plainImsi, String open5gsUri) {
        if (open5gsUri == null) return;
        try {
            open5gsProvisioning.deleteSubscriber(plainImsi, open5gsUri);
        } catch (Exception e) {
            log.warn("Failed to delete subscriber from Open5GS (will still remove from local DB): {}",
                    e.getMessage());
        }
    }

    public void deleteBulkQuietly(List<String> plainImsis, String open5gsUri) {
        if (open5gsUri == null) return;
        try {
            open5gsProvisioning.deleteSubscribersBulk(plainImsis, open5gsUri);
        } catch (Exception e) {
            log.warn("Batch Open5GS delete failed: {}", e.getMessage());
        }
    }
}
