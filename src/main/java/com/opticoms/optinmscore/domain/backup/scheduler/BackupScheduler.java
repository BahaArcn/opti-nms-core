package com.opticoms.optinmscore.domain.backup.scheduler;

import com.opticoms.optinmscore.domain.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupService backupService;

    @Scheduled(cron = "${app.backup.cron:0 0 3 * * *}")
    @SchedulerLock(name = "daily_backup", lockAtMostFor = "2h", lockAtLeastFor = "5m")
    public void scheduledBackup() {
        log.info("Starting scheduled daily backup");
        backupService.createBackup("SCHEDULER");
    }
}
