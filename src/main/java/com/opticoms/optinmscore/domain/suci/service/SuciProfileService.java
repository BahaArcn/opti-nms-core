package com.opticoms.optinmscore.domain.suci.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.suci.model.SuciProfile;
import com.opticoms.optinmscore.domain.suci.repository.SuciProfileRepository;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuciProfileService {

    private final SuciProfileRepository repository;
    private final EncryptionService encryptionService;

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]+$");

    @Audited(action = AuditAction.CREATE, entityType = "SuciProfile")
    public SuciProfile create(String tenantId, SuciProfile profile) {
        validateKeys(profile);
        checkDuplicateKeyId(tenantId, profile.getHomeNetworkPublicKeyId(), profile.getProtectionScheme(), null);

        profile.setTenantId(tenantId);
        profile.setKeyStatus(SuciProfile.KeyStatus.ACTIVE);
        encryptPrivateKey(profile);

        log.info("Creating SUCI profile: scheme={}, keyId={}, tenant={}",
                profile.getProtectionScheme(), profile.getHomeNetworkPublicKeyId(), tenantId);
        return repository.save(profile);
    }

    public SuciProfile getById(String tenantId, String id) {
        SuciProfile profile = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "SUCI profile not found: " + id));
        maskPrivateKey(profile);
        return profile;
    }

    public Page<SuciProfile> list(String tenantId, Pageable pageable) {
        Page<SuciProfile> page = repository.findByTenantId(tenantId, pageable);
        page.getContent().forEach(this::maskPrivateKey);
        return page;
    }

    public List<SuciProfile> listByStatus(String tenantId, SuciProfile.KeyStatus status) {
        List<SuciProfile> profiles = repository.findByTenantIdAndKeyStatus(tenantId, status);
        profiles.forEach(this::maskPrivateKey);
        return profiles;
    }

    public List<SuciProfile> listByScheme(String tenantId, SuciProfile.ProtectionScheme scheme) {
        List<SuciProfile> profiles = repository.findByTenantIdAndProtectionScheme(tenantId, scheme);
        profiles.forEach(this::maskPrivateKey);
        return profiles;
    }

    @Audited(action = AuditAction.UPDATE, entityType = "SuciProfile")
    public SuciProfile update(String tenantId, String id, SuciProfile updated) {
        SuciProfile existing = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "SUCI profile not found: " + id));

        if (existing.getKeyStatus() == SuciProfile.KeyStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot update a revoked SUCI profile. Create a new one instead.");
        }

        validateKeys(updated);
        checkDuplicateKeyId(tenantId, updated.getHomeNetworkPublicKeyId(),
                updated.getProtectionScheme(), id);

        existing.setProtectionScheme(updated.getProtectionScheme());
        existing.setHomeNetworkPublicKeyId(updated.getHomeNetworkPublicKeyId());
        existing.setHomeNetworkPublicKey(updated.getHomeNetworkPublicKey());
        if (updated.getHomeNetworkPrivateKey() != null) {
            existing.setHomeNetworkPrivateKey(updated.getHomeNetworkPrivateKey());
        }
        existing.setDescription(updated.getDescription());
        if (updated.getKeyStatus() != null) {
            existing.setKeyStatus(updated.getKeyStatus());
        }

        encryptPrivateKey(existing);
        log.info("Updating SUCI profile [{}]: scheme={}, keyId={}", id,
                existing.getProtectionScheme(), existing.getHomeNetworkPublicKeyId());
        return repository.save(existing);
    }

    @Audited(action = AuditAction.DELETE, entityType = "SuciProfile")
    public void delete(String tenantId, String id) {
        SuciProfile profile = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "SUCI profile not found: " + id));

        log.info("Deleting SUCI profile [{}]: scheme={}, keyId={}", id,
                profile.getProtectionScheme(), profile.getHomeNetworkPublicKeyId());
        repository.delete(profile);
    }

    @Audited(action = AuditAction.REVOKE, entityType = "SuciProfile")
    public SuciProfile revokeKey(String tenantId, String id) {
        SuciProfile profile = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "SUCI profile not found: " + id));

        if (profile.getKeyStatus() == SuciProfile.KeyStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Key is already revoked.");
        }

        profile.setKeyStatus(SuciProfile.KeyStatus.REVOKED);
        log.info("Revoking SUCI profile [{}]: scheme={}, keyId={}", id,
                profile.getProtectionScheme(), profile.getHomeNetworkPublicKeyId());
        return repository.save(profile);
    }

    public long count(String tenantId) {
        return repository.countByTenantId(tenantId);
    }

    public long countByStatus(String tenantId, SuciProfile.KeyStatus status) {
        return repository.countByTenantIdAndKeyStatus(tenantId, status);
    }

    private void validateKeys(SuciProfile profile) {
        if (profile.getProtectionScheme() == SuciProfile.ProtectionScheme.NULL_SCHEME) {
            return;
        }

        if (!HEX_PATTERN.matcher(profile.getHomeNetworkPublicKey()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Home Network Public Key must be a valid hex string.");
        }
        if (!HEX_PATTERN.matcher(profile.getHomeNetworkPrivateKey()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Home Network Private Key must be a valid hex string.");
        }

        int pubKeyBytes = profile.getHomeNetworkPublicKey().length() / 2;
        if (profile.getProtectionScheme() == SuciProfile.ProtectionScheme.PROFILE_A && pubKeyBytes != 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Profile A public key must be exactly 32 bytes (64 hex chars). Got " + pubKeyBytes + " bytes.");
        }
        if (profile.getProtectionScheme() == SuciProfile.ProtectionScheme.PROFILE_B && pubKeyBytes != 33) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Profile B public key must be exactly 33 bytes (66 hex chars, compressed secp256r1). Got " + pubKeyBytes + " bytes.");
        }
    }

    private void checkDuplicateKeyId(String tenantId, Integer keyId,
                                     SuciProfile.ProtectionScheme scheme, String excludeId) {
        repository.findByTenantIdAndHomeNetworkPublicKeyIdAndProtectionScheme(tenantId, keyId, scheme)
                .ifPresent(existing -> {
                    if (excludeId == null || !existing.getId().equals(excludeId)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "A SUCI profile with keyId=" + keyId + " and scheme=" + scheme
                                        + " already exists for this tenant.");
                    }
                });
    }

    private void encryptPrivateKey(SuciProfile profile) {
        if (profile.getHomeNetworkPrivateKey() != null) {
            profile.setHomeNetworkPrivateKey(
                    encryptionService.encrypt(profile.getHomeNetworkPrivateKey()));
        }
    }

    private void maskPrivateKey(SuciProfile profile) {
        profile.setHomeNetworkPrivateKey(null);
    }

    private void decryptPrivateKey(SuciProfile profile) {
        if (profile.getHomeNetworkPrivateKey() != null) {
            profile.setHomeNetworkPrivateKey(
                    encryptionService.decrypt(profile.getHomeNetworkPrivateKey()));
        }
    }
}
