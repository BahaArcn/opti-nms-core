package com.opticoms.optinmscore.integration.open5gs.deploy.renderer;

import com.opticoms.optinmscore.domain.network.model.GlobalConfig;
import com.opticoms.optinmscore.domain.suci.model.SuciProfile;
import com.opticoms.optinmscore.domain.suci.repository.SuciProfileRepository;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GlobalConfig → ausf/udm/udr/bsf/pcf/scp YAML string'leri üretir.
 *
 * LLD Parametre → YAML Path:
 *   GlobalConfig.maxSupportedDevices → global.max.ue  (tüm NF'ler)
 *
 * UDM hnet bölümü: Eğer SuciProfileRepository mevcutsa ve tenant'a ait
 * ACTIVE profiller varsa, hnet bölümü DB'den dinamik üretilir.
 * Profil yoksa hardcoded fallback (6 key) kullanılır.
 *
 * Desteklenen NF'ler: ausf, udm, udr, bsf, pcf, scp
 */
@Component
public class CommonNfYamlRenderer {

    private final Yaml yaml;
    private final SuciProfileRepository suciProfileRepository;

    public CommonNfYamlRenderer(SuciProfileRepository suciProfileRepository) {
        this.suciProfileRepository = suciProfileRepository;
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setExplicitStart(false);
        this.yaml = new Yaml(opts);
    }

    /** No-arg constructor for tests that don't need repository integration. */
    public CommonNfYamlRenderer() {
        this(null);
    }

    /**
     * Tüm common NF'ler için YAML üretir.
     *
     * @param global GlobalConfig — max.ue için
     * @return Map: NF adı → YAML string
     *         Örnek: {"ausf" → "logger:...", "udm" → "logger:...", ...}
     */
    public Map<String, String> renderAll(GlobalConfig global, String tenantId) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("ausf", renderAusf(global));
        result.put("udm",  renderUdm(global, tenantId));
        result.put("udr",  renderUdr(global));
        result.put("bsf",  renderBsf(global));
        result.put("pcf",  renderPcf(global));
        result.put("scp",  renderScp(global));
        return result;
    }

    /** Backward-compatible overload — uses hardcoded hnet defaults. */
    public Map<String, String> renderAll(GlobalConfig global) {
        return renderAll(global, null);
    }

    /**
     * AUSF — Authentication Server Function
     * 5G UE kimlik doğrulama — UDM ile konuşur.
     *
     * BUG-03 düzeltmesi: Referans ausf-configmap.yaml'da SBI server
     * "dev: eth0" değil "address: 0.0.0.0" kullanıyor.
     */
    public String renderAusf(GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("logger", buildLogger("ausf"));
        root.put("global", buildGlobalSection(global));

        Map<String, Object> ausf = new LinkedHashMap<>();
        ausf.put("sbi", buildSbiWithAddress("0.0.0.0", "ausf-nausf", List.of(
                buildScpClient()
        )));
        root.put("ausf", ausf);

        return yaml.dump(root);
    }

    /**
     * UDM — Unified Data Management
     * Subscriber verilerini yönetir — UDR'den veri çeker.
     *
     * BUG-02 düzeltmesi: hnet bölümü eklendi.
     * SUCI (Subscription Concealed Identifier) şifre çözme için
     * UDM'nin HNET private key dosyalarına erişmesi gerekir.
     * Key dosyaları K8s volume'da sabit yollarda mount edilir.
     * Referans: udm-configmap.yaml — 6 key (curve25519 + secp256r1 çiftleri)
     */
    public String renderUdm(GlobalConfig global, String tenantId) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("logger", buildLogger("udm"));
        root.put("global", buildGlobalSection(global));

        Map<String, Object> udm = new LinkedHashMap<>();
        udm.put("hnet", buildHnetSection(tenantId));
        udm.put("sbi", buildSbi("udm-nudm", List.of(
                buildScpClient()
        )));
        root.put("udm", udm);

        return yaml.dump(root);
    }

    /** Backward-compatible overload — uses hardcoded hnet defaults. */
    public String renderUdm(GlobalConfig global) {
        return renderUdm(global, null);
    }

    /**
     * UDR — Unified Data Repository
     * Subscriber datasının gerçek deposu — MongoDB ile konuşur.
     *
     * BUG-01 düzeltmesi: db_uri eklendi.
     * UDR, subscriber verilerini MongoDB'den okur/yazar.
     * db_uri olmadan UDR start-up sırasında MongoDB'e bağlanamaz.
     * Referans: udr-configmap.yaml — db_uri ilk alan olarak tanımlı.
     */
    public String renderUdr(GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("db_uri", "mongodb://mongodb/open5gs");
        root.put("logger", buildLogger("udr"));
        root.put("global", buildGlobalSection(global));

        Map<String, Object> udr = new LinkedHashMap<>();
        udr.put("sbi", buildSbi("udr-nudr", List.of(
                buildScpClient()
        )));
        root.put("udr", udr);

        return yaml.dump(root);
    }

    /**
     * BSF — Binding Support Function
     * UE'nin hangi PCF'e bağlı olduğunu takip eder.
     */
    public String renderBsf(GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("logger", buildLogger("bsf"));
        root.put("global", buildGlobalSection(global));

        Map<String, Object> bsf = new LinkedHashMap<>();
        bsf.put("sbi", buildSbi("bsf-nbsf", List.of(
                buildScpClient()
        )));
        root.put("bsf", bsf);

        return yaml.dump(root);
    }

    /**
     * PCF — Policy Control Function
     * UE policy kararlarını yönetir — MongoDB'den policy verisi okur.
     *
     * Özellikleri:
     * - db_uri: MongoDB bağlantısı (K8s servis DNS ile)
     * - metrics.server: 0.0.0.0:9090 (Prometheus scrape endpoint'i)
     * - SCP üzerinden diğer NF'lere erişir
     */
    public String renderPcf(GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("db_uri", "mongodb://mongodb/open5gs");
        root.put("logger", buildLogger("pcf"));
        root.put("global", buildGlobalSection(global));

        Map<String, Object> pcf = new LinkedHashMap<>();
        pcf.put("sbi", buildSbi("pcf-npcf", List.of(buildScpClient())));

        // Metrics: Prometheus scrape endpoint'i
        Map<String, Object> metricsServer = new LinkedHashMap<>();
        metricsServer.put("address", "0.0.0.0");
        metricsServer.put("port", 9090);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("server", List.of(metricsServer));
        pcf.put("metrics", metrics);

        root.put("pcf", pcf);
        return yaml.dump(root);
    }

    /**
     * SCP — Service Communication Proxy
     * NF'ler arası SBI trafiğini proxy'ler — NRF ile iletişim kurar.
     *
     * Özellikleri:
     * - no_sepp: true (SEPP olmadan çalışır, single-PLMN deployment için)
     * - NRF client: SCP'nin kendisi NRF'e kayıt olur
     */
    public String renderScp(GlobalConfig global) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("logger", buildLogger("scp"));
        root.put("global", buildGlobalSection(global));

        Map<String, Object> scp = new LinkedHashMap<>();

        // SBI: server + NRF client (SCP, SCP client kullanmaz — kendisi proxy)
        Map<String, Object> sbi = new LinkedHashMap<>();

        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", "eth0");
        server.put("advertise", "scp-nscp");
        server.put("port", 80);
        servers.add(server);
        sbi.put("server", servers);

        // SCP, NRF'e doğrudan bağlanır (SCP client değil, NRF client)
        Map<String, Object> nrfEntry = new LinkedHashMap<>();
        nrfEntry.put("uri", "http://nrf-nnrf:80");
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("nrf", List.of(nrfEntry));
        sbi.put("client", client);

        scp.put("sbi", sbi);
        scp.put("no_sepp", true);
        root.put("scp", scp);
        return yaml.dump(root);
    }

    // ── Private builder metodları ────────────────────────────────────────────

    /**
     * global.max bölümü — LLD Tablo 3
     * GlobalConfig.maxSupportedDevices → global.max.ue
     * GlobalConfig.maxSupportedGNBs   → global.max.gnb
     */
    private Map<String, Object> buildGlobalSection(GlobalConfig global) {
        Map<String, Object> globalSection = new LinkedHashMap<>();
        Map<String, Object> maxMap = new LinkedHashMap<>();
        maxMap.put("ue", global.getMaxSupportedDevices());
        maxMap.put("gnb", global.getMaxSupportedGNBs());
        globalSection.put("max", maxMap);
        return globalSection;
    }

    private Map<String, Object> buildLogger(String nfName) {
        Map<String, Object> logger = new LinkedHashMap<>();
        logger.put("file", "/open5gs/install/var/log/open5gs/" + nfName + ".log");
        return logger;
    }

    /**
     * SBI section — dev:eth0 ile (UDM, UDR, BSF, PCF, SCP için)
     *
     * @param advertise K8s service DNS adı (örn. "udm-nudm")
     * @param clients   sbi.client altına eklenecek clientlar
     */
    private Map<String, Object> buildSbi(String advertise, List<Map<String, Object>> clients) {
        Map<String, Object> sbi = new LinkedHashMap<>();

        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("dev", "eth0");
        server.put("advertise", advertise);
        server.put("port", 80);
        servers.add(server);
        sbi.put("server", servers);

        if (clients != null && !clients.isEmpty()) {
            Map<String, Object> client = new LinkedHashMap<>();
            client.put("scp", clients);
            sbi.put("client", client);
        }

        return sbi;
    }

    /**
     * SBI section — address ile (AUSF için)
     * Referans ausf-configmap.yaml'da "dev: eth0" yerine "address: 0.0.0.0" kullanılıyor.
     *
     * @param address   SBI server bind adresi (örn. "0.0.0.0")
     * @param advertise K8s service DNS adı (örn. "ausf-nausf")
     * @param clients   sbi.client altına eklenecek clientlar
     */
    private Map<String, Object> buildSbiWithAddress(String address, String advertise,
                                                     List<Map<String, Object>> clients) {
        Map<String, Object> sbi = new LinkedHashMap<>();

        List<Map<String, Object>> servers = new ArrayList<>();
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("address", address);
        server.put("advertise", advertise);
        server.put("port", 80);
        servers.add(server);
        sbi.put("server", servers);

        if (clients != null && !clients.isEmpty()) {
            Map<String, Object> client = new LinkedHashMap<>();
            client.put("scp", clients);
            sbi.put("client", client);
        }

        return sbi;
    }

    /**
     * UDM hnet bölümü — SUCI şifre çözme için HNET private key dosyaları.
     *
     * Scheme 1 = Profile-A (curve25519), Scheme 2 = Profile-B (secp256r1)
     * Key dosyaları K8s volume'da mount edilen sabit yollarda tutulur.
     *
     * Eğer {@link SuciProfileRepository} inject edilmişse ve tenant'a ait ACTIVE
     * profiller varsa, hnet bölümü MongoDB'deki profillerden dinamik üretilir.
     * Yoksa hardcoded 6-key fallback kullanılır.
     */
    private List<Map<String, Object>> buildHnetSection(String tenantId) {
        if (suciProfileRepository != null && tenantId != null) {
            List<SuciProfile> activeProfiles = suciProfileRepository
                    .findByTenantIdAndKeyStatus(tenantId, SuciProfile.KeyStatus.ACTIVE);
            if (!activeProfiles.isEmpty()) {
                return buildHnetFromProfiles(activeProfiles);
            }
        }
        return buildHnetFallback();
    }

    private List<Map<String, Object>> buildHnetFromProfiles(List<SuciProfile> profiles) {
        String basePath = "/open5gs/install/etc/open5gs/hnet/";
        List<Map<String, Object>> hnetList = new ArrayList<>();
        for (SuciProfile p : profiles) {
            int scheme = p.getProtectionScheme() == SuciProfile.ProtectionScheme.PROFILE_A ? 1 : 2;
            String curveName = scheme == 1 ? "curve25519" : "secp256r1";
            String keyFile = basePath + curveName + "-" + p.getHomeNetworkPublicKeyId() + ".key";

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", p.getHomeNetworkPublicKeyId());
            entry.put("scheme", scheme);
            entry.put("key", keyFile);
            hnetList.add(entry);
        }
        return hnetList;
    }

    private List<Map<String, Object>> buildHnetFallback() {
        String basePath = "/open5gs/install/etc/open5gs/hnet/";
        Object[][] keys = {
            {1, 1, basePath + "curve25519-1.key"},
            {2, 2, basePath + "secp256r1-2.key"},
            {3, 1, basePath + "curve25519-3.key"},
            {4, 2, basePath + "secp256r1-4.key"},
            {5, 1, basePath + "curve25519-5.key"},
            {6, 2, basePath + "secp256r1-6.key"},
        };

        List<Map<String, Object>> hnetList = new ArrayList<>();
        for (Object[] k : keys) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", k[0]);
            entry.put("scheme", k[1]);
            entry.put("key", k[2]);
            hnetList.add(entry);
        }
        return hnetList;
    }

    private Map<String, Object> buildScpClient() {
        Map<String, Object> scp = new LinkedHashMap<>();
        scp.put("uri", "http://scp-nscp:80");
        return scp;
    }
}
