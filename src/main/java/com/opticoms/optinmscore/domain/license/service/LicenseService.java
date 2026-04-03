package com.opticoms.optinmscore.domain.license.service;

import com.opticoms.optinmscore.domain.apn.repository.ApnProfileRepository;
import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.edgelocation.repository.EdgeLocationRepository;
import com.opticoms.optinmscore.domain.inventory.repository.GNodeBRepository;
import com.opticoms.optinmscore.domain.license.model.License;
import com.opticoms.optinmscore.domain.license.repository.LicenseRepository;
import com.opticoms.optinmscore.domain.subscriber.repository.SubscriberRepository;
import com.opticoms.optinmscore.domain.system.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final SubscriberRepository subscriberRepository;
    private final GNodeBRepository gNodeBRepository;
    private final ApnProfileRepository apnProfileRepository;
    private final EdgeLocationRepository edgeLocationRepository;
    private final UserRepository userRepository;

    @Audited(action = AuditAction.CREATE, entityType = "License")
    public License createOrUpdateLicense(String tenantId, License license) {
        Optional<License> existing = licenseRepository.findByTenantId(tenantId);

        if (existing.isPresent()) {
            License current = existing.get();
            current.setLicenseKey(license.getLicenseKey());
            current.setMaxSubscribers(license.getMaxSubscribers());
            current.setMaxGNodeBs(license.getMaxGNodeBs());
            current.setMaxDnns(license.getMaxDnns());
            current.setMaxEdgeLocations(license.getMaxEdgeLocations());
            current.setMaxUsers(license.getMaxUsers());
            current.setExpiresAt(license.getExpiresAt());
            current.setActive(license.isActive());
            current.setDescription(license.getDescription());
            log.info("Updating license for tenant={}", tenantId);
            return licenseRepository.save(current);
        }

        license.setTenantId(tenantId);
        log.info("Creating license for tenant={}", tenantId);
        return licenseRepository.save(license);
    }

    public License getLicense(String tenantId) {
        return licenseRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No license found for tenant: " + tenantId));
    }

    @Audited(action = AuditAction.DELETE, entityType = "License")
    public void deleteLicense(String tenantId) {
        License license = getLicense(tenantId);
        licenseRepository.delete(license);
        log.info("Deleted license for tenant={}", tenantId);
    }

    public void checkCanAddSubscriber(String tenantId) {
        checkLicenseLimit(tenantId, "subscribers",
                subscriberRepository.countByTenantId(tenantId),
                lic -> lic.getMaxSubscribers());
    }

    /**
     * Returns the number of additional subscribers this tenant can add.
     * <p>
     * NOTE: This check is not atomic. Two concurrent bulk imports could both
     * read the same remaining quota and collectively exceed the limit.
     * Acceptable for current single-tenant deployment. For multi-tenant,
     * consider MongoDB atomic counter or distributed locking.
     *
     * @return remaining quota, or {@link Integer#MAX_VALUE} if unlimited
     */
    public int getRemainingSubscriberQuota(String tenantId) {
        Optional<License> optLicense = licenseRepository.findByTenantId(tenantId);

        if (optLicense.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        License license = optLicense.get();

        if (!license.isActive()) {
            return 0;
        }

        if (license.getExpiresAt() != null && license.getExpiresAt() < System.currentTimeMillis()) {
            return Integer.MAX_VALUE;
        }

        Integer limit = license.getMaxSubscribers();
        if (limit == null) {
            return Integer.MAX_VALUE;
        }

        long currentCount = subscriberRepository.countByTenantId(tenantId);
        return Math.max(0, (int) (limit - currentCount));
    }

    public void checkCanAddDnn(String tenantId) {
        checkLicenseLimit(tenantId, "DNN profiles",
                apnProfileRepository.countByTenantId(tenantId),
                lic -> lic.getMaxDnns());
    }

    public void checkCanAddGNodeB(String tenantId) {
        checkLicenseLimit(tenantId, "gNodeBs",
                gNodeBRepository.countByTenantId(tenantId),
                lic -> lic.getMaxGNodeBs());
    }

    public void checkCanAddEdgeLocation(String tenantId) {
        checkLicenseLimit(tenantId, "edge locations",
                edgeLocationRepository.countByTenantId(tenantId),
                lic -> lic.getMaxEdgeLocations());
    }

    public LicenseStatus getLicenseStatus(String tenantId) {
        Optional<License> optLicense = licenseRepository.findByTenantId(tenantId);

        LicenseStatus status = new LicenseStatus();
        if (optLicense.isEmpty()) {
            status.setLicensePresent(false);
            status.setActive(false);
            status.setExpired(false);
            return status;
        }

        License license = optLicense.get();
        boolean expired = license.getExpiresAt() != null
                && license.getExpiresAt() < System.currentTimeMillis();

        status.setLicensePresent(true);
        status.setActive(license.isActive() && !expired);
        status.setExpired(expired);
        status.setLicenseKey(license.getLicenseKey());
        status.setMaxSubscribers(license.getMaxSubscribers());
        status.setMaxGNodeBs(license.getMaxGNodeBs());
        status.setMaxDnns(license.getMaxDnns());
        status.setMaxEdgeLocations(license.getMaxEdgeLocations());
        status.setMaxUsers(license.getMaxUsers());
        status.setExpiresAt(license.getExpiresAt());
        status.setCurrentSubscribers(subscriberRepository.countByTenantId(tenantId));
        status.setCurrentGNodeBs(gNodeBRepository.countByTenantId(tenantId));
        status.setCurrentDnns(apnProfileRepository.countByTenantId(tenantId));
        status.setCurrentEdgeLocations(edgeLocationRepository.countByTenantId(tenantId));
        status.setCurrentUsers(userRepository.countByTenantId(tenantId));

        return status;
    }

    private void checkLicenseLimit(String tenantId, String resourceName,
                                   long currentCount, LimitExtractor extractor) {
        Optional<License> optLicense = licenseRepository.findByTenantId(tenantId);
        if (optLicense.isEmpty()) {
            return; // no license = unlimited
        }

        License license = optLicense.get();

        if (!license.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "License is not active for this tenant");
        }

        if (license.getExpiresAt() != null && license.getExpiresAt() < System.currentTimeMillis()) {
            return; // expired = grace period, limits not enforced
        }

        Integer limit = extractor.getLimit(license);
        if (limit != null && currentCount >= limit) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No sufficient licenses to add " + resourceName
                            + ". Current: " + currentCount + ", Max: " + limit);
        }
    }

    @FunctionalInterface
    private interface LimitExtractor {
        Integer getLimit(License license);
    }

    @Data
    public static class LicenseStatus {
        private boolean licensePresent;
        private boolean active;
        private boolean expired;
        private String licenseKey;
        private Integer maxSubscribers;
        private Integer maxGNodeBs;
        private Integer maxDnns;
        private Integer maxEdgeLocations;
        private Integer maxUsers;
        private Long expiresAt;
        private long currentSubscribers;
        private long currentGNodeBs;
        private long currentDnns;
        private long currentEdgeLocations;
        private long currentUsers;
    }
}
