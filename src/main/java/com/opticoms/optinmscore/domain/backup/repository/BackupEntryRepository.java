package com.opticoms.optinmscore.domain.backup.repository;

import com.opticoms.optinmscore.domain.backup.model.BackupEntry;
import com.opticoms.optinmscore.domain.backup.model.BackupEntry.BackupStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BackupEntryRepository extends MongoRepository<BackupEntry, String> {

    List<BackupEntry> findAllByOrderByCreatedAtDesc();

    List<BackupEntry> findByStatusOrderByCreatedAtDesc(BackupStatus status);
}
