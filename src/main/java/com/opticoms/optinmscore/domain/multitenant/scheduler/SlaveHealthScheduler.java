package com.opticoms.optinmscore.domain.multitenant.scheduler;

import com.opticoms.optinmscore.domain.multitenant.model.SlaveNode;
import com.opticoms.optinmscore.domain.multitenant.repository.SlaveNodeRepository;
import com.opticoms.optinmscore.domain.multitenant.service.MasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlaveHealthScheduler {

    private final SlaveNodeRepository slaveNodeRepository;
    private final MasterService masterService;

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "slave_health_check", lockAtMostFor = "2m", lockAtLeastFor = "20s")
    public void markStaleSlaves() {
        Set<String> tenantIds = slaveNodeRepository.findAll().stream()
                .map(SlaveNode::getTenantId)
                .collect(Collectors.toSet());

        for (String tenantId : tenantIds) {
            int marked = masterService.markStaleSlaves(tenantId);
            if (marked > 0) {
                log.info("Marked {} stale slave(s) as OFFLINE for tenant={}", marked, tenantId);
            }
        }
    }
}
