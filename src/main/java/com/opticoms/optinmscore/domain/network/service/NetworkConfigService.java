package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import com.opticoms.optinmscore.domain.network.repository.AmfConfigRepository;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import com.opticoms.optinmscore.domain.network.repository.SmfConfigRepository;
import com.opticoms.optinmscore.domain.network.repository.UpfConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkConfigService {

    private final GlobalConfigRepository globalConfigRepository;
    private final AmfConfigRepository amfConfigRepository;
    private final SmfConfigRepository smfConfigRepository;
    private final UpfConfigRepository upfConfigRepository;

    @Audited(action = AuditAction.UPDATE, entityType = "GlobalConfig")
    public GlobalConfig saveOrUpdateGlobalConfig(String tenantId, GlobalConfig newConfig) {

        validateUeIpPoolList(newConfig.getUeIpPoolList());

        Optional<GlobalConfig> existingConfigOpt = globalConfigRepository.findByTenantId(tenantId);

        if (existingConfigOpt.isPresent()) {
            GlobalConfig existingConfig = existingConfigOpt.get();

            if (existingConfig.getNetworkMode() != newConfig.getNetworkMode()) {
                log.info("Network mode changed for tenant {}: {} → {}",
                        tenantId, existingConfig.getNetworkMode(), newConfig.getNetworkMode());
                validateModeChangeCompatibility(tenantId, newConfig.getNetworkMode());
            }

            newConfig.setId(existingConfig.getId());
            newConfig.setVersion(existingConfig.getVersion());
            newConfig.setCreatedAt(existingConfig.getCreatedAt());
            newConfig.setCreatedBy(existingConfig.getCreatedBy());
            newConfig.setMaxSupportedDevices(existingConfig.getMaxSupportedDevices());
            newConfig.setMaxSupportedGNBs(existingConfig.getMaxSupportedGNBs());
        }

        newConfig.setTenantId(tenantId);
        return globalConfigRepository.save(newConfig);
    }

    public GlobalConfig getGlobalConfig(String tenantId) {
        return globalConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Global Configuration not found for tenant: " + tenantId));
    }

    /**
     * When switching network mode, verify existing AMF/SMF/UPF configs
     * have the fields required by the new mode. Rejects the change if
     * existing configs would become invalid.
     */
    private void validateModeChangeCompatibility(String tenantId, GlobalConfig.NetworkMode newMode) {
        boolean needs5g = newMode == GlobalConfig.NetworkMode.ONLY_5G
                || newMode == GlobalConfig.NetworkMode.HYBRID_4G_5G;
        boolean needs4g = newMode == GlobalConfig.NetworkMode.ONLY_4G
                || newMode == GlobalConfig.NetworkMode.HYBRID_4G_5G;

        List<String> warnings = new ArrayList<>();

        amfConfigRepository.findByTenantId(tenantId).ifPresent(amf -> {
            if (needs5g) {
                if (amf.getAmfName() == null || amf.getAmfName().isBlank())
                    warnings.add("AMF config missing amfName (required for 5G)");
                if (amf.getAmfId() == null)
                    warnings.add("AMF config missing amfId (required for 5G)");
                if (amf.getSupportedSlices() == null || amf.getSupportedSlices().isEmpty())
                    warnings.add("AMF config missing supportedSlices (required for 5G)");
            }
            if (needs4g) {
                if (amf.getMmeName() == null || amf.getMmeName().isBlank())
                    warnings.add("AMF config missing mmeName (required for 4G)");
                if (amf.getMmeId() == null)
                    warnings.add("AMF config missing mmeId (required for 4G)");
            }
        });

        upfConfigRepository.findByTenantId(tenantId).ifPresent(upf -> {
            if (needs5g && isBlank(upf.getN3InterfaceIp()))
                warnings.add("UPF config missing n3InterfaceIp (required for 5G)");
            if (needs4g && isBlank(upf.getS1uInterfaceIp()))
                warnings.add("UPF config missing s1uInterfaceIp (required for 4G)");
        });

        if (!warnings.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot switch to " + newMode + " — existing configs are incompatible: "
                            + String.join("; ", warnings)
                            + ". Update AMF/SMF/UPF configs first, then change the network mode.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void validateUeIpPoolList(List<GlobalConfig.UeIpPool> pools) {
        if (pools == null || pools.isEmpty()) {
            return;
        }

        Set<String> seenTunInterfaces = new HashSet<>();
        for (GlobalConfig.UeIpPool pool : pools) {
            if (!seenTunInterfaces.add(pool.getTunInterface())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate tunInterface: '" + pool.getTunInterface() + "'");
            }
        }

        for (int i = 0; i < pools.size(); i++) {
            for (int j = i + 1; j < pools.size(); j++) {
                if (cidrOverlaps(pools.get(i).getIpRange(), pools.get(j).getIpRange())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "IP range overlap between '" + pools.get(i).getIpRange()
                                    + "' (" + pools.get(i).getTunInterface()
                                    + ") and '" + pools.get(j).getIpRange()
                                    + "' (" + pools.get(j).getTunInterface() + ")");
                }
            }
        }
    }

    static boolean cidrOverlaps(String rangeA, String rangeB) {
        if (!rangeA.contains("/") || !rangeB.contains("/")) {
            return false;
        }
        try {
            long[] netA = parseCidr(rangeA);
            long[] netB = parseCidr(rangeB);
            long commonMask = netA[1] & netB[1];
            return (netA[0] & commonMask) == (netB[0] & commonMask);
        } catch (Exception e) {
            return false;
        }
    }

    private static long[] parseCidr(String cidr) throws UnknownHostException {
        String[] parts = cidr.split("/");
        byte[] addr = InetAddress.getByName(parts[0]).getAddress();
        long ip = ((long) (addr[0] & 0xFF) << 24)
                | ((long) (addr[1] & 0xFF) << 16)
                | ((long) (addr[2] & 0xFF) << 8)
                | (addr[3] & 0xFF);
        int prefix = Integer.parseInt(parts[1]);
        long mask = prefix == 0 ? 0L : 0xFFFFFFFFL << (32 - prefix);
        return new long[]{ip & mask, mask};
    }
}