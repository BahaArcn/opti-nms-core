package com.opticoms.optinmscore.domain.system.update.controller;

import com.opticoms.optinmscore.domain.system.update.dto.UpdateCheckResult;
import com.opticoms.optinmscore.domain.system.update.dto.VersionInfo;
import com.opticoms.optinmscore.domain.system.update.service.UpdateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System - Auto Update")
@RequiredArgsConstructor
public class SystemUpdateController {

    private final UpdateService updateService;

    @GetMapping("/version")
    public ResponseEntity<VersionInfo> getVersion() {
        return ResponseEntity.ok(updateService.getVersion());
    }

    @PostMapping("/update/check")
    public ResponseEntity<UpdateCheckResult> checkForUpdate() {
        return ResponseEntity.ok(updateService.checkForUpdate());
    }

    @PostMapping("/update/apply")
    public ResponseEntity<Map<String, String>> applyUpdate() {
        return ResponseEntity.ok(updateService.applyUpdate());
    }
}
