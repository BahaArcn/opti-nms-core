# OptiNMS Core — Mimari Denetim Raporu V2

**Tarih:** 6 Nisan 2026  
**Kapsam:** 213 production Java dosyası, 87 test dosyası, Dockerfile, Helm chart, CI/CD pipeline, konfigürasyon dosyaları  
**Durum:** Spring Boot 3.2.12 + MongoDB + Java 17  
**Denetçi:** Cursor (Senior Software Architect / Staff Engineer seviyesinde)

---

## 1. Yönetici Özeti

OptiNMS Core, telekomünikasyon sektörüne özgü ciddi bir 5G NMS backend'idir. Domain-driven klasör yapısı, MapStruct DTO katmanı, AES-256-GCM şifreleme, JWT + RBAC güvenlik modeli, çoklu tenant desteği ve %64 test coverage ile ortalamanın **üzerinde** bir yazılım olgunluğuna sahiptir.

**Güçlü yanlar:** Kapsamlı RBAC kuralları, HMAC-SHA256 keyed hash, PBKDF2 key derivation, DTO ile domain ayrımı, ShedLock ile dağıtık zamanlayıcı kilidi, multi-stage Dockerfile, Helm secret doğrulaması, OWASP dependency check entegrasyonu, 702 geçen test.

**Zayıf yanlar:** Birkaç ciddi güvenlik açığı (backup restore'da authorization bypass riski, PBKDF2 static salt), bazı servisler için test boşluğu (%0 coverage: TenantService, PolicyService, AuditAspect), ve operasyonel olgunluk eksikleri (graceful shutdown backup sırasında, Kubernetes RBAC eksikliği).

---

## 2. Kritik Bulgular

### CRIT-01: BackupService.restoreBackup() — Tenant İzolasyonu Yok

**Dosya:** `domain/backup/service/BackupService.java:115-140`

`restoreBackup` tüm 23 collection'ı `dropCollection` + `insert` ile silip yeniden yazıyor. Bu, **tüm tenant'ların verisini** tek bir SUPER_ADMIN'in kontrolüne bırakıyor. Bir tenant'ın backup'ından restore yapıldığında, diğer tenant'ların canlı verisi kaybolur. Ayrıca `backup_entries` collection'ın kendisi korunmuyor — restore sırasında `users` collection'ı da override ediliyor, bu da SUPER_ADMIN hesabının kendisini bile silme riski taşıyor.

**Öneri:** Restore öncesi `users` collection'ında mevcut SUPER_ADMIN'in korunduğunu doğrula. Alternatif olarak restore'u tenant-scoped yap.

---

### CRIT-02: EncryptionService — Statik PBKDF2 Salt

**Dosya:** `security/encryption/EncryptionService.java:33`

```java
private static final byte[] PBKDF2_SALT = "OptiNMS-Salt-v1".getBytes(StandardCharsets.UTF_8);
```

Sabit salt kullanımı, aynı master key'e sahip tüm deployment'larda aynı derived key'i üretir. Saldırgan bu salt'ı bildiği için, pre-computation saldırıları kolaylaşır. 100.000 iterasyon iyi bir rakam ama salt deterministik olmamalı.

**Öneri:** Deployment-specific rastgele salt üret ve birinci kullanımda persist et (MongoDB veya dosya sistemi). Mevcut şifrelenmiş verilerin geriye dönük uyumluluğu için migrasyon mekanizması gerekir.

---

### CRIT-03: `enrichWithConnectionStatus` — N+1 Benzeri Yük

**Dosya:** `domain/subscriber/service/SubscriberService.java:182-196`

Her sayfalı subscriber listesinde, **tenant'ın tüm connected UE'leri** belleğe çekilip Map'e konuyor:

```java
Map<String, ConnectedUe> ueMap = connectedUeRepository.findByTenantId(tenantId)
        .stream().collect(...);
```

10.000 aktif UE'si olan bir tenant için, 20'lik sayfa isteğinde bile 10.000 döküman belleğe yükleniyor. Bu lineer olarak büyüyen bir performans sorunudur.

**Öneri:** Sayfadaki subscriber IMSI'lerini toplayıp `findByTenantIdAndImsiIn(tenantId, imsiList)` ile sadece ilgili UE'leri çek.

---

### CRIT-04: EncryptionService — RuntimeException Standardizasyonu

**Dosya:** `security/encryption/EncryptionService.java:39, 50, 73, 94, 119`

5 farklı yerde catch-all `RuntimeException` fırlatılıyor. Spring'in `@PostConstruct` içinde RuntimeException fırlatmak uygulamayı durduruyor (doğru), ama production'da hangi hata olduğunu anlamak zor. Özellikle `encrypt`/`decrypt`/`hash` metodlarındaki RuntimeException'lar uygulama çökmesi yerine anlamlı hata mesajı vermeli.

**Öneri:** `EncryptionException extends RuntimeException` sınıfı oluştur; `GlobalExceptionHandler`'da bu exception'ı 500 olarak yakala.

---

## 3. Orta Öncelikli Bulgular

### MED-01: Test Coverage Boşlukları — Kritik Servisler %0

JaCoCo raporuna göre:

| Paket | Coverage | Risk |
|-------|----------|------|
| `domain.tenant.service` | %1 | URI şifreleme/çözme hiç test edilmemiş |
| `domain.policy.service` | %0 | PolicyService CRUD |
| `domain.audit.aspect` | %0 | Tüm audit logging mekanizması |
| `domain.performance.service` | %18 | Aggregation pipeline |
| `integration.open5gs` | %34 | Provisioning, sync |

Bu servisler iş mantığının çekirdeğini oluşturuyor. TenantService URI şifrelemesinin hiç test edilmemesi, production'da veri kaybı riski taşıyor.

---

### MED-02: UserController — DTO Tutarsızlığı

**Dosya:** `domain/system/controller/UserController.java:166-187`

Tüm projede MapStruct kullanılırken, `UserController` hâlâ inline `UserResponse.from(user)` static factory method kullanıyor. `CreateUserRequest`, `UpdateRoleRequest` vs. de inner static class olarak controller'ın içinde tanımlanmış. Proje genelinde DTO'lar ayrı `dto/` paketlerinde ve MapStruct mapper'larla eşleniyor — bu tek dosya tutarsız.

**Öneri:** `domain/system/dto/UserResponse.java`, `UserRequest.java` + `UserMapper.java` oluştur, mevcut pattern'e uyumlu hale getir.

---

### MED-03: BackupService — Tek Thread'de Tüm Collection'lar

**Dosya:** `domain/backup/service/BackupService.java:75-82`

23 collection sırayla tek thread'de okunuyor ve tek bir GZIP dosyasına yazılıyor. Büyük bir deployment'da (100K subscriber, milyonlarca PM metric) bu işlem onlarca dakika sürebilir ve `BackupScheduler`'ın ShedLock `lockAtMostFor = "30m"` limitini aşabilir.

**Öneri:** `pm_metrics` ve `audit_logs` gibi büyüyebilecek collection'lar için `mongoTemplate.stream()` kullan. Alternatif olarak `lockAtMostFor`'u collection boyutuna göre dinamik yap veya streaming GZIP yazımına geç.

---

### MED-04: AuditAspect — tenantId Çıkarım Zayıflığı

**Dosya:** `domain/audit/aspect/AuditAspect.java:86-90`

Request context'den tenantId bulunamazsa, `args[0]`'ın String olduğunu ve bunun tenantId olduğunu varsayıyor. Bu argüman sırası convention'ına bağımlı, kırılgan bir yaklaşım. Yeni bir `@Audited` metod ilk parametre olarak String olmayan bir değer alırsa, yanlış tenantId loglanır.

**Öneri:** `@Audited` annotation'ına opsiyonel `tenantIdArgIndex` parametresi ekle veya `TenantContext`'i her durumda kullan.

---

### MED-05: CORS `allowedOrigins` Split Mantığı

**Dosya:** `config/SecurityConfiguration.java:180`

```java
configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
```

`allowedOrigins` değeri `*` ise, `List.of("*")` olarak geçer. `allowedOriginPatterns("*")` + `allowCredentials(true)` kombinasyonu her origin'e credentials ile erişim verir. Bu dev'de kabul edilebilir ama production'da `application-prod.yml`'daki `APP_CORS_ALLOWED_ORIGINS` env var'ının set edilmemesi durumunda sessiz güvenlik açığı yaratır.

**Öneri:** `prod` profile'da CORS allowed origins boş veya `*` ise uygulama başlamamalı (`@PostConstruct` kontrolü).

---

### MED-06: MapStruct Mapper'lar — %0 Coverage (Yapay Düşüş)

14 adet MapStruct mapper implementation'ı (generated code) JaCoCo'da %0 coverage gösteriyor. Generated code'un test edilmesi gerekmez ama JaCoCo exclude listesine eklenmemesi, genel coverage metriğini yapay olarak düşürüyor.

**Öneri:** `pom.xml`'daki JaCoCo configuration'a mapper implementation paketlerini exclude et:
```xml
<excludes>
    <exclude>**/mapper/*MapperImpl.class</exclude>
</excludes>
```

---

### MED-07: `application.yml` — Dev Ortamında `show-details: always`

**Dosya:** `application.yml:60`

Default profile'da actuator health detayları herkese açık (`show-details: always`). Bu, MongoDB bağlantı bilgileri ve disk kullanımı dahil iç yapıyı ifşa eder. `application-prod.yml` bunu `when-authorized` yapıyor ama dev profilini production'da kullanan biri bilgi sızdırır.

---

### MED-08: Subscriber Model — Ki/OPc Alanları Mapper Exclude Doğrulaması Yok

`SubscriberResponse` DTO'sunda `ki`, `opc`, `op` alanları yok (doğru). Ancak mapper implementation'ının bu alanları gerçekten exclude ettiğini doğrulayan bir test yok. Mapper regeneration sonrasında bu alanlar kazara dahil olabilir.

**Öneri:** `SubscriberControllerTest`'e "response body should not contain ki/opc/op fields" assertion'ı ekle.

---

## 4. Düşük Öncelikli İyileştirmeler

### LOW-01: `pom.xml` — Türkçe Yorumlar Kaldı

**Dosya:** `pom.xml:96, 100, 103`

```xml
<!-- SnakeYAML: Renderer katmanında Map -> YAML string üretimi için -->
<!-- Fabric8 Kubernetes Client: ConfigMap update + Deployment rollout restart için -->
```

Backlog'da Türkçe yorumlar temizlenmiş deniyordu ama `pom.xml`'de 3 Türkçe yorum kaldı.

---

### LOW-02: `OptiNmsCoreApplicationTests.contextLoads` — Sürekli Başarısız

Bu test MongoDB olmadan çalışamıyor. CI'da MongoDB var ama lokal'de her zaman 1 error gösteriyor. Test ya `@SpringBootTest(webEnvironment = MOCK)` + `@MockBean` ile yazılmalı, ya da CI'a özel profile'a taşınmalı.

---

### LOW-03: `BackupEntry.tenantId` — BaseEntity Regex Uyumsuzluğu

`BackupEntry` model'inde `tenantId` BaseEntity'den miras alınıyor ve `BackupService`'de her zaman `"SYSTEM"` olarak set ediliyor. `BaseEntity.tenantId` üzerinde `@Pattern(regexp = "^[A-Z]{4}-\\d{4}/\\d{4}/\\d{2}$")` regex var — `"SYSTEM"` bu regex'e uymuyor. Şu an validation server-side trigger olmuyor ama ileride sorun yaratabilir.

---

### LOW-04: Helm Chart — `_helpers.tpl` Yok

`helm/templates/` altında `_helpers.tpl` (template helper fonksiyonları) dosyası yok. `deployment.yaml`'da `{{ .Chart.Name }}` doğrudan kullanılıyor. Helm best practice'e göre `{{ include "opti-nms.fullname" . }}` helper'ı `_helpers.tpl`'de tanımlanmalı.

---

### LOW-05: Dockerfile — Build Optimizasyonu

`mvn package -DskipTests` yerine `mvn package -DskipTests -Dmaven.javadoc.skip=true` kullanılması build süresini kısaltır.

---

### LOW-06: InMemoryRateLimiter — Fixed-Window Burst Problemi

Javadoc'ta belirtilmiş: pencere sınırında 2x burst mümkün. Auth endpoint'ler için bu, brute force korumasını zayıflatır. Sliding window veya token bucket daha güvenli olurdu ama mevcut `authRequestsPerWindow=5` ile kabul edilebilir seviyede.

---

## 5. Öncelik Sırasıyla Refactor Edilecek Dosyalar/Modüller

| Sıra | Dosya/Modül | Sebep |
|------|------------|-------|
| 1 | `BackupService.java` | CRIT-01: restore'da tenant izolasyonu ve SUPER_ADMIN koruması yok |
| 2 | `EncryptionService.java` | CRIT-02: statik salt, CRIT-04: RuntimeException standardizasyonu |
| 3 | `SubscriberService.java:182-196` | CRIT-03: enrichWithConnectionStatus N+1 yükü |
| 4 | `TenantService` + test | MED-01: %1 coverage — şifreleme mantığı test edilmemiş |
| 5 | `AuditAspect.java` + test | MED-01/MED-04: %0 coverage + kırılgan tenantId çıkarımı |
| 6 | `UserController.java` | MED-02: DTO/mapper tutarsızlığı |
| 7 | Generated mapper excludes | MED-06: JaCoCo'da yapay coverage düşüşü |

---

## 6. Somut Öneriler (Dosya Referanslı)

### 6.1 BackupService — Restore Güvenliği

**Dosya:** `BackupService.java:115`

`restoreBackup()` başlangıcına:
- Mevcut `users` collection'ından SUPER_ADMIN hesaplarını kopyala
- Restore sonrası bu hesapları geri yaz
- `backup_entries` collection'ı restore kapsamından hariç tut (zaten excluded ama restore edilen JSON'da varsa sorun olur)

### 6.2 EncryptionService — Rastgele Salt

**Dosya:** `EncryptionService.java:33`

Deployment-specific salt için:
- MongoDB'de `encryption_metadata` collection'ına bir döküman ekle
- İlk başlatmada 16-byte `SecureRandom` salt üret ve kaydet
- Sonraki başlatmalarda bu salt'ı oku

### 6.3 SubscriberService — Connection Status Optimizasyonu

**Dosya:** `SubscriberService.java:183`

Değiştir:
```java
List<String> pageImsis = subscribers.stream()
    .map(Subscriber::getImsi).toList();
Map<String, ConnectedUe> ueMap = connectedUeRepository
    .findByTenantIdAndImsiIn(tenantId, pageImsis)
    .stream().collect(...);
```

### 6.4 JaCoCo Mapper Exclude

`pom.xml` JaCoCo configuration'a:
```xml
<excludes>
    <exclude>**/mapper/*MapperImpl.class</exclude>
</excludes>
```

### 6.5 Test Önceliği

Sırasıyla test yazılması gereken sınıflar:

1. `TenantServiceTest` — `encryptUri`/`decryptUri` round-trip, backward compat
2. `AuditAspectTest` — `extractTenantId` edge case'leri
3. `PolicyServiceTest` — CRUD + tenant isolation
4. `PmServiceTest` — aggregation pipeline doğruluğu
5. `SubscriberServiceTest` — `enrichWithConnectionStatus` mock'lu test

---

## 7. Mimari Puan Kartı

| Alan | Puan | Notlar |
|------|------|--------|
| **Klasör Yapısı** | 4.5/5 | Domain-driven, tutarlı `model/service/controller/dto/mapper/repository` pattern'i. `integration/` paketi harici bağımlılıkları ayırıyor. |
| **Kod Organizasyonu** | 4/5 | SubscriberService iyi bölünmüş (Service/SyncService/Helper). UserController DTO tutarsızlığı tek zayıf nokta. |
| **İsimlendirme** | 4.5/5 | Tutarlı: `*Service`, `*Controller`, `*Repository`, `*Mapper`, `*Response`, `*Request`. Endpoint'ler RESTful. |
| **Tip Güvenliği** | 4/5 | MapStruct DTO katmanı, `@Valid` validation, enum kullanımı. `SessionProfile` iç sınıf olarak model'de — ayrı class olsa daha iyi. |
| **Hata Yönetimi** | 3.5/5 | `GlobalExceptionHandler` kapsamlı ama `RuntimeException` yerine custom exception'lar olmalı. `ResponseStatusException` yaygın kullanımı iyi. |
| **Güvenlik** | 4/5 | AES-256-GCM, HMAC-SHA256, PBKDF2, JWT + RBAC, rate limiting, SSRF koruması, constant-time token karşılaştırma (`MessageDigest.isEqual`). Statik salt ve backup restore zayıf noktalar. |
| **Test Coverage** | 3.5/5 | 702 test, %64 coverage. Auth testleri, security integration testleri güçlü. Ama 5+ kritik servis %0-1 coverage. |
| **Performans** | 3.5/5 | Pagination doğru uygulanmış, `findAll` kaldırılmış, TTL index var, aggregation pipeline var. `enrichWithConnectionStatus` N+1 sorunu mevcut. |
| **CI/CD** | 4/5 | GitHub Actions: MongoDB service container, JaCoCo report, OWASP check, Docker build/push. Eksik: staging environment, E2E test step, Helm lint. |
| **Bakım Kolaylığı** | 4/5 | Tutarlı pattern'ler, Lombok boilerplate azaltımı, MapStruct mapping, anlamlı log mesajları. `@Audited` aspect güzel bir cross-cutting concern çözümü. |
| **Endüstri Uyumu** | 4/5 | Spring Boot best practice'lerine büyük ölçüde uygun. Helm chart, multi-stage Docker, non-root user, JVM container flags, graceful shutdown. |

**Genel Ortalama: 3.95 / 5**

---

## 8. Nihai Değerlendirme

### Verdict: Kabul Edilebilir → Güçlü arası (leaning strong)

Bu proje, 5G NMS gibi niş bir alanda ciddi mühendislik çabası gösteriyor. Mimari kararlar (domain-driven paketleme, DTO katmanı, encryption servisi, multi-tenancy, RBAC, rate limiting) doğru yönde ve büyük çoğunluğu endüstri standardında uygulanmış.

Production'a çıkmadan önce ele alınması gereken **3 blocker**:

1. **CRIT-01:** Backup restore'da tenant/user koruması
2. **CRIT-02:** Statik PBKDF2 salt'ın deployment-specific hale getirilmesi
3. **CRIT-03:** `enrichWithConnectionStatus` N+1 sorununun düzeltilmesi

Bu üçü çözüldüğünde proje **"güçlü"** kategorisine geçer.

**Tahmini düzeltme süreleri:**

| Bulgu | Süre |
|-------|------|
| CRIT-01 (Backup restore güvenliği) | 2 saat |
| CRIT-02 (Deployment-specific salt + migration) | 4 saat |
| CRIT-03 (N+1 connection status) | 30 dakika |
| MED-01 (Kritik servis testleri) | 1 gün |
| Toplam tahmini | ~2 gün |

---

## 9. Özet Tablo

| Seviye | Adet | ID'ler |
|--------|------|--------|
| Kritik | 4 | CRIT-01, CRIT-02, CRIT-03, CRIT-04 |
| Orta | 8 | MED-01 → MED-08 |
| Düşük | 6 | LOW-01 → LOW-06 |
| **Toplam** | **18** | |
