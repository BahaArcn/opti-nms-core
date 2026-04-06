package com.opticoms.optinmscore.domain.backup.controller;

import com.opticoms.optinmscore.domain.backup.dto.BackupResponse;
import com.opticoms.optinmscore.domain.backup.mapper.BackupMapper;
import com.opticoms.optinmscore.domain.backup.model.BackupEntry;
import com.opticoms.optinmscore.domain.backup.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/backup")
@RequiredArgsConstructor
@Tag(name = "Backup Management", description = "System backup and restore operations (SUPER_ADMIN only)")
public class BackupController {

    private final BackupService backupService;
    private final BackupMapper backupMapper;

    @Operation(summary = "Trigger a manual backup")
    @PostMapping("/trigger")
    public ResponseEntity<BackupResponse> triggerBackup(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "UNKNOWN";
        BackupEntry entry = backupService.createBackup(username);
        return ResponseEntity.ok(backupMapper.toResponse(entry));
    }

    @Operation(summary = "List all backups")
    @GetMapping
    public ResponseEntity<List<BackupResponse>> listBackups() {
        return ResponseEntity.ok(backupMapper.toResponseList(backupService.listBackups()));
    }

    @Operation(summary = "Download a backup file")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadBackup(@PathVariable String id) {
        byte[] data = backupService.getBackupFile(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"backup.json.gz\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(data);
    }

    @Operation(summary = "Restore system from a backup")
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreBackup(@PathVariable String id) {
        backupService.restoreBackup(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete a backup")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBackup(@PathVariable String id) {
        backupService.deleteBackup(id);
        return ResponseEntity.noContent().build();
    }
}
