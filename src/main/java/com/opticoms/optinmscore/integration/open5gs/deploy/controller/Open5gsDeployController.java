package com.opticoms.optinmscore.integration.open5gs.deploy.controller;

import com.opticoms.optinmscore.common.util.TenantContext;
import com.opticoms.optinmscore.integration.open5gs.deploy.dto.DeployResult;
import com.opticoms.optinmscore.integration.open5gs.deploy.dto.RenderedConfigs;
import com.opticoms.optinmscore.integration.open5gs.deploy.service.ConfigRenderService;
import com.opticoms.optinmscore.integration.open5gs.deploy.service.KubernetesDeployService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Open5GS Kubernetes deploy endpoint'leri.
 *
 * Güvenlik: Tüm endpoint'ler sadece ROLE_ADMIN erişimine açık.
 * @PreAuthorize, SecurityConfiguration'daki @EnableMethodSecurity ile aktif.
 *
 * Akış:
 *   HTTP POST → Controller → ConfigRenderService (DB→YAML) → KubernetesDeployService (K8s apply)
 *
 * Tenant identity is derived from the JWT token for each request.
 */
@RestController
@RequestMapping("/api/v1/open5gs/deploy")
@RequiredArgsConstructor
@Tag(name = "Open5GS Deploy", description = "Open5GS Kubernetes ConfigMap deploy ve rollout restart işlemleri")
public class Open5gsDeployController {

    private final ConfigRenderService renderService;
    private final KubernetesDeployService deployService;

    /**
     * Tüm NF'leri (AMF + SMF + UPF + NRF + NSSF) deploy eder.
     *
     * İş akışı:
     * 1. DB'den GlobalConfig + AmfConfig + SmfConfig + UpfConfig çek.
     * 2. Tüm NF YAML'larını üret.
     * 3. open5gs namespace'indeki ConfigMap'leri güncelle.
     * 4. Tüm Deployment'lara rollout restart uygula.
     */
    @Operation(summary = "Deploy all NFs", description = "Tüm Open5GS NF'lerini DB'deki güncel config ile deploy eder")
    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeployResult> deployAll(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        RenderedConfigs rendered = renderService.renderAll(tenantId);
        DeployResult result = deployService.applyAll(rendered);
        return ResponseEntity.ok(result);
    }

    /**
     * Sadece AMF'i deploy eder.
     * AMF config değişince AMF'i restart etmek yeterli — diğer NF'lere dokunmaz.
     */
    @Operation(summary = "Deploy AMF only")
    @PostMapping("/amf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeployResult> deployAmf(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        RenderedConfigs rendered = renderService.renderAmfOnly(tenantId);
        DeployResult result = deployService.applyAmf(rendered);
        return ResponseEntity.ok(result);
    }

    /**
     * Sadece SMF'i deploy eder.
     */
    @Operation(summary = "Deploy SMF only")
    @PostMapping("/smf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeployResult> deploySmf(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        RenderedConfigs rendered = renderService.renderSmfOnly(tenantId);
        DeployResult result = deployService.applySmf(rendered);
        return ResponseEntity.ok(result);
    }

    /**
     * Sadece UPF'i deploy eder.
     * UPF deploy'u SmfConfig'e de bağımlı (subnet/dnn için) — renderUpfOnly ikisinide çeker.
     */
    @Operation(summary = "Deploy UPF only")
    @PostMapping("/upf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeployResult> deployUpf(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        RenderedConfigs rendered = renderService.renderUpfOnly(tenantId);
        DeployResult result = deployService.applyUpf(rendered);
        return ResponseEntity.ok(result);
    }

    /**
     * Dry-run: Tüm NF YAML'larını üretir ama K8s'e uygulamaz.
     *
     * Kullanım amacı: Deploy öncesi config'in doğruluğunu kontrol etmek.
     * Frontend bu endpoint'i çağırarak üretilecek YAML'ları kullanıcıya gösterebilir.
     *
     * Response: { "amfcfg.yaml": "...", "smfcfg.yaml": "...", "upfcfg.yaml": "...", ... }
     */
    @Operation(summary = "Preview all rendered YAMLs (dry-run)",
               description = "YAML'ları üretir ama K8s'e uygulamaz. Config doğrulama için kullanın.")
    @GetMapping("/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> preview(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        RenderedConfigs rendered = renderService.renderAll(tenantId);

        // Frontend'in okuyabileceği düz map: dosya adı → YAML içeriği
        Map<String, String> preview = new LinkedHashMap<>();
        preview.put("amfcfg.yaml",   rendered.getAmfYaml());
        preview.put("smfcfg.yaml",   rendered.getSmfYaml());
        preview.put("upfcfg.yaml",   rendered.getUpfYaml());
        preview.put("wrapper.sh",    rendered.getWrapperSh());
        preview.put("nrfcfg.yaml",   rendered.getNrfYaml());
        preview.put("nssfcfg.yaml",  rendered.getNssfYaml());

        // Common NFs: ausf / udm / udr / bsf / pcf / scp
        if (rendered.getCommonNfYamls() != null) {
            rendered.getCommonNfYamls().forEach(
                    (nf, yamlContent) -> preview.put(nf + "cfg.yaml", yamlContent));
        }

        // 4G / HYBRID mod: MME YAML
        if (rendered.getMmeYaml() != null) {
            preview.put("mmecfg.yaml", rendered.getMmeYaml());
        }

        return ResponseEntity.ok(preview);
    }
}
