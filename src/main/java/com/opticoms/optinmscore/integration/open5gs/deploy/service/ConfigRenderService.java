package com.opticoms.optinmscore.integration.open5gs.deploy.service;

import com.opticoms.optinmscore.domain.network.model.AmfConfig;
import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.network.model.SmfConfig;
import com.opticoms.optinmscore.domain.network.model.UpfConfig;
import com.opticoms.optinmscore.domain.network.service.AmfConfigService;
import com.opticoms.optinmscore.domain.network.service.NetworkConfigService;
import com.opticoms.optinmscore.domain.network.service.SmfConfigService;
import com.opticoms.optinmscore.domain.network.service.UpfConfigService;
import com.opticoms.optinmscore.integration.open5gs.deploy.dto.RenderedConfigs;
import com.opticoms.optinmscore.integration.open5gs.deploy.renderer.*;

import static com.opticoms.optinmscore.domain.network.model.GlobalConfig.NetworkMode.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates YAML renderers for Open5GS network functions.
 *
 * Loads configs via {@code *ConfigService} from MongoDB, invokes the appropriate renderer,
 * and wraps results in {@link com.opticoms.optinmscore.integration.open5gs.deploy.dto.RenderedConfigs}.
 * Controllers depend on this service only, so the rendering strategy can change without controller edits.
 */
@Service
@RequiredArgsConstructor
public class ConfigRenderService {

    private final NetworkConfigService networkConfigService;
    private final AmfConfigService amfConfigService;
    private final SmfConfigService smfConfigService;
    private final UpfConfigService upfConfigService;

    private final AmfYamlRenderer amfRenderer;
    private final SmfYamlRenderer smfRenderer;
    private final UpfYamlRenderer upfRenderer;
    private final NrfYamlRenderer nrfRenderer;
    private final NssfYamlRenderer nssfRenderer;
    private final CommonNfYamlRenderer commonNfRenderer;
    private final MmeYamlRenderer mmeRenderer;
    private final SgwuYamlRenderer sgwuRenderer;

    /**
     * Renders YAML for all network functions ({@code /deploy/all}, {@code /preview}).
     *
     * @param tenantId JWT-derived tenant identifier
     */
    public RenderedConfigs renderAll(String tenantId) {
        GlobalConfig global = networkConfigService.getGlobalConfig(tenantId);
        AmfConfig amf       = amfConfigService.getAmfConfig(tenantId);
        SmfConfig smf       = smfConfigService.getSmfConfig(tenantId);
        UpfConfig upf       = upfConfigService.getUpfConfig(tenantId);

        RenderedConfigs.RenderedConfigsBuilder builder = RenderedConfigs.builder()
                .amfYaml(amfRenderer.render(global, amf))
                .smfYaml(smfRenderer.render(smf, global, upf))
                .upfYaml(upfRenderer.renderYaml(upf, smf, global))
                .wrapperSh(upfRenderer.renderWrapperScript(smf, global))
                .nrfYaml(nrfRenderer.render(amf, global))
                .nssfYaml(nssfRenderer.render(amf, global))
                .commonNfYamls(commonNfRenderer.renderAll(global, tenantId));

        if (global.getNetworkMode() == ONLY_4G || global.getNetworkMode() == HYBRID_4G_5G) {
            builder.mmeYaml(mmeRenderer.render(amf, global));
            builder.sgwuYaml(sgwuRenderer.render(upf, global));
        }

        return builder.build();
    }

    /** Renders AMF YAML only ({@code /deploy/amf}). */
    public RenderedConfigs renderAmfOnly(String tenantId) {
        GlobalConfig global = networkConfigService.getGlobalConfig(tenantId);
        AmfConfig amf       = amfConfigService.getAmfConfig(tenantId);

        return RenderedConfigs.builder()
                .amfYaml(amfRenderer.render(global, amf))
                .build();
    }

    /** Renders SMF YAML only (loads UPF for PFCP client address). */
    public RenderedConfigs renderSmfOnly(String tenantId) {
        GlobalConfig global = networkConfigService.getGlobalConfig(tenantId);
        SmfConfig smf       = smfConfigService.getSmfConfig(tenantId);
        UpfConfig upf       = upfConfigService.getUpfConfig(tenantId);

        return RenderedConfigs.builder()
                .smfYaml(smfRenderer.render(smf, global, upf))
                .build();
    }

    /**
     * Renders UPF YAML and wrapper.sh (Gap 3: requires SmfConfig as well as UpfConfig).
     */
    public RenderedConfigs renderUpfOnly(String tenantId) {
        GlobalConfig global = networkConfigService.getGlobalConfig(tenantId);
        SmfConfig smf       = smfConfigService.getSmfConfig(tenantId);
        UpfConfig upf       = upfConfigService.getUpfConfig(tenantId);

        return RenderedConfigs.builder()
                .upfYaml(upfRenderer.renderYaml(upf, smf, global))
                .wrapperSh(upfRenderer.renderWrapperScript(smf, global))
                .build();
    }
}
