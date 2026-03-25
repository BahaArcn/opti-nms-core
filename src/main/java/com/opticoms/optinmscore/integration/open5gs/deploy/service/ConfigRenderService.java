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
 * Renderer katmanını orkestre eder.
 *
 * Sorumlulukları:
 * 1. Mevcut *ConfigService sınıflarını kullanarak MongoDB'den config'leri çeker.
 * 2. İlgili Renderer sınıfına doğru config nesnelerini iletir.
 * 3. Sonuçları RenderedConfigs DTO'suna paketler.
 *
 * Controller bu servisi çağırır; renderer'ları doğrudan görmez.
 * Bu sayede ileride farklı bir render stratejisi (Freemarker, template dosyası)
 * eklenirse controller kodu değişmez.
 */
@Service
@RequiredArgsConstructor
public class ConfigRenderService {

    // Mevcut CRUD servisleri — DB erişimi bunlar üzerinden
    private final NetworkConfigService networkConfigService;   // GlobalConfig
    private final AmfConfigService amfConfigService;
    private final SmfConfigService smfConfigService;
    private final UpfConfigService upfConfigService;

    // Renderer'lar — her NF için bağımsız
    private final AmfYamlRenderer amfRenderer;
    private final SmfYamlRenderer smfRenderer;
    private final UpfYamlRenderer upfRenderer;
    private final NrfYamlRenderer nrfRenderer;
    private final NssfYamlRenderer nssfRenderer;
    private final CommonNfYamlRenderer commonNfRenderer;
    private final MmeYamlRenderer mmeRenderer;
    private final SgwuYamlRenderer sgwuRenderer;

    /**
     * Tüm NF'ler için YAML üretir.
     * deploy/all ve preview endpoint'leri bu metodu çağırır.
     *
     * @param tenantId JWT-derived tenant identifier
     * @return Tüm NF YAML'larını içeren RenderedConfigs
     */
    public RenderedConfigs renderAll(String tenantId) {
        // DB'den en güncel config'leri çek (desired-state mantığı)
        GlobalConfig global = networkConfigService.getGlobalConfig(tenantId);
        AmfConfig amf       = amfConfigService.getAmfConfig(tenantId);
        SmfConfig smf       = smfConfigService.getSmfConfig(tenantId);
        UpfConfig upf       = upfConfigService.getUpfConfig(tenantId);

        RenderedConfigs.RenderedConfigsBuilder builder = RenderedConfigs.builder()
                .amfYaml(amfRenderer.render(global, amf))
                .smfYaml(smfRenderer.render(smf, global, upf))
                .upfYaml(upfRenderer.renderYaml(upf, smf, global))
                .wrapperSh(upfRenderer.renderWrapperScript(smf))
                .nrfYaml(nrfRenderer.render(amf, global))
                .nssfYaml(nssfRenderer.render(amf, global))
                .commonNfYamls(commonNfRenderer.renderAll(global, tenantId));

        if (global.getNetworkMode() == ONLY_4G || global.getNetworkMode() == HYBRID_4G_5G) {
            builder.mmeYaml(mmeRenderer.render(amf, global));
            builder.sgwuYaml(sgwuRenderer.render(upf, global));
        }

        return builder.build();
    }

    /**
     * Sadece AMF için YAML üretir.
     * deploy/amf endpoint'i bu metodu çağırır.
     */
    public RenderedConfigs renderAmfOnly(String tenantId) {
        GlobalConfig global = networkConfigService.getGlobalConfig(tenantId);
        AmfConfig amf       = amfConfigService.getAmfConfig(tenantId);

        return RenderedConfigs.builder()
                .amfYaml(amfRenderer.render(global, amf))
                .build();
    }

    /**
     * Sadece SMF için YAML üretir.
     */
    public RenderedConfigs renderSmfOnly(String tenantId) {
        GlobalConfig global = networkConfigService.getGlobalConfig(tenantId);
        SmfConfig smf       = smfConfigService.getSmfConfig(tenantId);
        UpfConfig upf       = upfConfigService.getUpfConfig(tenantId); // PFCP address için

        return RenderedConfigs.builder()
                .smfYaml(smfRenderer.render(smf, global, upf))
                .build();
    }

    /**
     * Sadece UPF için YAML + wrapper.sh üretir.
     * Gap 3: UPF renderer SmfConfig'e de bağımlı, ikisi de çekiliyor.
     */
    public RenderedConfigs renderUpfOnly(String tenantId) {
        GlobalConfig global = networkConfigService.getGlobalConfig(tenantId);
        SmfConfig smf       = smfConfigService.getSmfConfig(tenantId);
        UpfConfig upf       = upfConfigService.getUpfConfig(tenantId);

        return RenderedConfigs.builder()
                .upfYaml(upfRenderer.renderYaml(upf, smf, global))
                .wrapperSh(upfRenderer.renderWrapperScript(smf))
                .build();
    }
}
