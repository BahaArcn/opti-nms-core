package com.opticoms.optinmscore.domain.network.service;

import com.opticoms.optinmscore.domain.audit.aspect.Audited;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.repository.GlobalConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NetworkConfigService {

    // Dependency Injection (Bağımlılık Enjeksiyonu)
    // @RequiredArgsConstructor sayesinde Spring bu repository'yi bizim için otomatik olarak "new"ler.
    private final GlobalConfigRepository globalConfigRepository;

    /**
     * İş Kuralı 1: Müşterinin ayarını kaydet veya zaten varsa GÜNCELLE.
     */
    @Audited(action = AuditAction.UPDATE, entityType = "GlobalConfig")
    public GlobalConfig saveOrUpdateGlobalConfig(String tenantId, GlobalConfig newConfig) {

        validateUeIpPoolList(newConfig.getUeIpPoolList());

        Optional<GlobalConfig> existingConfigOpt = globalConfigRepository.findByTenantId(tenantId);

        if (existingConfigOpt.isPresent()) {
            GlobalConfig existingConfig = existingConfigOpt.get();
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

    /**
     * İş Kuralı 2: Müşterinin ayarını getir. Yoksa hata fırlat.
     */
    public GlobalConfig getGlobalConfig(String tenantId) {
        return globalConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Global Configuration not found for tenant: " + tenantId));
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