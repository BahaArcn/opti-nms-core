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
 * Open5GS Kubernetes deploy API.
 *
 * All routes require {@code ROLE_ADMIN} ({@code @PreAuthorize} with method security enabled).
 *
 * Flow: HTTP POST → {@link ConfigRenderService} (DB → YAML) → {@link KubernetesDeployService} (apply to cluster).
 * Tenant identity comes from the JWT per request.
 */
@RestController
@RequestMapping("/api/v1/open5gs/deploy")
@RequiredArgsConstructor
@Tag(name = "Open5GS Deploy", description = "Open5GS ConfigMap updates and deployment rollouts on Kubernetes")
public class Open5gsDeployController {

    private final ConfigRenderService renderService;
    private final KubernetesDeployService deployService;

    /** Deploys all NFs (AMF, SMF, UPF, NRF, NSSF, common NFs, and 4G NFs when applicable). */
    @Operation(summary = "Deploy all NFs", description = "Deploy all Open5GS network functions using current config from the database")
    @PostMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeployResult> deployAll(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        RenderedConfigs rendered = renderService.renderAll(tenantId);
        DeployResult result = deployService.applyAll(rendered);
        return ResponseEntity.ok(result);
    }

    /** Deploys AMF only (restart AMF when AMF config changes). */
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

    /** Deploys SMF only. */
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
     * Deploys UPF only. Rendering also loads SmfConfig (subnets/DNNs); {@link ConfigRenderService#renderUpfOnly} pulls both.
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
     * Dry-run: renders all NF YAML strings without applying to Kubernetes.
     * Response shape: {@code { "amfcfg.yaml": "...", "smfcfg.yaml": "...", ... }}
     */
    @Operation(summary = "Preview all rendered YAMLs (dry-run)",
               description = "Render YAML only; does not apply to Kubernetes. Use to validate config before deploy.")
    @GetMapping("/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> preview(
            HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId(request);
        RenderedConfigs rendered = renderService.renderAll(tenantId);

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

        if (rendered.getMmeYaml() != null) {
            preview.put("mmecfg.yaml", rendered.getMmeYaml());
        }

        return ResponseEntity.ok(preview);
    }
}
