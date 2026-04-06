package com.opticoms.optinmscore.domain.subscriber.service;

import com.opticoms.optinmscore.domain.apn.model.ApnProfile;
import com.opticoms.optinmscore.domain.apn.repository.ApnProfileRepository;
import com.opticoms.optinmscore.domain.policy.service.PolicyService;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
class SubscriberHelper {

    private final EncryptionService encryptionService;
    private final ApnProfileRepository apnProfileRepository;
    private final PolicyService policyService;
    private final TenantService tenantService;

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]+$");

    /**
     * For each SessionProfile in the subscriber, if only apnDnn is provided,
     * look up the matching ApnProfile and fill in sst, sd, QoS, AMBR, pduType
     * automatically so the user doesn't have to enter them manually.
     */
    void enrichProfilesFromApn(String tenantId, Subscriber subscriber) {
        if (subscriber.getProfileList() == null) {
            return;
        }
        for (Subscriber.SessionProfile profile : subscriber.getProfileList()) {
            if (profile.getApnDnn() == null || profile.getApnDnn().isBlank()) {
                continue;
            }
            var apnOpt = apnProfileRepository.findFirstByTenantIdAndDnnAndEnabledTrue(
                    tenantId, profile.getApnDnn());
            if (apnOpt.isEmpty()) {
                continue;
            }
            ApnProfile apn = apnOpt.get();

            if (profile.getSst() == 0) {
                profile.setSst(apn.getSst());
            }
            if (profile.getSd() == null || "FFFFFF".equalsIgnoreCase(profile.getSd())) {
                if (apn.getSd() != null && !apn.getSd().isBlank()) {
                    profile.setSd(apn.getSd());
                }
            }
            if (profile.getQi5g() == 0 && apn.getQos() != null && apn.getQos().getFiveQi() != null) {
                profile.setQi5g(apn.getQos().getFiveQi());
            }
            if (profile.getQci4g() == 0 && apn.getQos() != null && apn.getQos().getFiveQi() != null) {
                profile.setQci4g(apn.getQos().getFiveQi());
            }
            if (profile.getArpPriority() == 0 && apn.getQos() != null && apn.getQos().getArpPriorityLevel() != null) {
                profile.setArpPriority(apn.getQos().getArpPriorityLevel());
            }
            if (apn.getQos() != null) {
                if (apn.getQos().getPreEmptionCapability() == ApnProfile.PreEmption.PRE_EMPT) {
                    profile.setPreemptionCapability(true);
                }
                if (apn.getQos().getPreEmptionVulnerability() == ApnProfile.PreEmption.PRE_EMPTABLE) {
                    profile.setPreemptionVulnerability(true);
                }
            }
            if (profile.getPduType() == 0 && apn.getPduSessionType() != null) {
                profile.setPduType(switch (apn.getPduSessionType()) {
                    case IPV4 -> 1;
                    case IPV6 -> 2;
                    case IPV4V6 -> 3;
                });
            }
            if (profile.getSessionAmbrDl() == 0 && apn.getSessionAmbr() != null && apn.getSessionAmbr().getDownlinkKbps() != null) {
                profile.setSessionAmbrDl(apn.getSessionAmbr().getDownlinkKbps() * 1000);
            }
            if (profile.getSessionAmbrUl() == 0 && apn.getSessionAmbr() != null && apn.getSessionAmbr().getUplinkKbps() != null) {
                profile.setSessionAmbrUl(apn.getSessionAmbr().getUplinkKbps() * 1000);
            }
        }
    }

    void validatePolicyReference(String tenantId, Subscriber sub) {
        if (sub.getPolicyId() != null && !sub.getPolicyId().isBlank()) {
            if (!policyService.existsForTenant(tenantId, sub.getPolicyId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Referenced policy not found: " + sub.getPolicyId());
            }
        }
    }

    void validateKeys(Subscriber sub) {
        validateHex(sub.getKi(), 16, "Ki");
        if (sub.getUsimType() == Subscriber.UsimType.OPC) {
            validateHex(sub.getOpc(), 16, "OPc");
        } else if (sub.getUsimType() == Subscriber.UsimType.OP) {
            validateHex(sub.getOp(), 16, "OP");
        }
    }

    void encryptSensitiveData(Subscriber sub) {
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

    void decryptSensitiveData(Subscriber sub) {
        sub.setImsi(encryptionService.decrypt(sub.getImsi()));
        if (sub.getMsisdn() != null && !sub.getMsisdn().isEmpty()) {
            sub.setMsisdn(encryptionService.decrypt(sub.getMsisdn()));
        }
        sub.setKi(encryptionService.decrypt(sub.getKi()));
        if (sub.getOpc() != null) sub.setOpc(encryptionService.decrypt(sub.getOpc()));
        if (sub.getOp() != null) sub.setOp(encryptionService.decrypt(sub.getOp()));
    }

    void validateHex(String value, int requiredBytes, String fieldName) {
        if (value == null || value.length() != (requiredBytes * 2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must be exactly " + requiredBytes + " bytes (" + (requiredBytes * 2) + " hex chars)");
        }
        if (!HEX_PATTERN.matcher(value).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must contain only hexadecimal characters (0-9, A-F)");
        }
    }

    String resolveOpen5gsUri(String tenantId) {
        try {
            return tenantService.getTenant(tenantId).getOpen5gsMongoUri();
        } catch (ResponseStatusException e) {
            return null;
        }
    }
}
