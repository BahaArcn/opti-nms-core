# OptiNMS Core Backend — Kapsamlı Mimari Denetim Raporu

**Tarih:** 3 Nisan 2026 (güncelleme: 6 Nisan 2026)
**Proje:** OptiNMS Core Backend (Java 17 + Spring Boot 3.2.3 + MongoDB)
**Kapsam:** 152 Java kaynak dosyası, 61 test dosyası (~536 test metodu), 17 domain modülü
**Denetim kaynakları:** İç denetim + harici mimari denetim (birleştirilmiş)

---

## 1. Yönetici Özeti

OptiNMS, Java 17 + Spring Boot 3.2.3 + MongoDB üzerine kurulmuş bir **5G Network Management System** backend'idir. Proje **152 Java kaynak dosyası**, **61 test dosyası** (~536 test metodu) ve **17 domain modülü** içerir. Feature-based paket yapısı (controller → service → repository katmanlama) büyük ölçüde tutarlı uygulanmış, güvenlik JWT + RBAC + tenant izolasyonu üzerine inşa edilmiştir.

**Güçlü yanlar:** Temiz modül yapısı, tutarlı multi-tenant mimari, kapsamlı exception handling, AES-256-GCM şifreleme, ShedLock ile scheduler korunması, JaCoCo %60 coverage zorunluluğu, CI/CD pipeline, RestTemplate timeout konfigürasyonu (`RestTemplateConfig.java` — connect 5s, read 10s).

**Zayıf yanlar:** Master/slave güvenlik mimarisi sahada çalışmıyor, DTO katmanı yok (domain modeller doğrudan expose), kriptografik hash tasarımı zayıf (saltsız SHA-256), SSRF riski (kontrolsüz slaveAddress), performans riskleri (sınırlandırılmamış liste sorguları, N+1 pattern'leri, senkron dashboard I/O), ve Helm chart'ta secret değerler repo'da.

---

## 2. Kritik Sorunlar

### CRIT-01 — Master/Slave Güvenlik Akışı Çelişkisi (Sahada Çalışmaz)

- **Dosyalar:** `SecurityConfiguration.java`, `MasterController.java`, `SlaveClientService.java`, `MasterTokenFilterConfig.java`
- `MasterController` endpoint'leri (`/api/v1/master/slaves/**`) JWT ile korunan genel güvenlik zincirine dahil ve `hasRole("ADMIN")` gerektiriyor. Ancak `SlaveClientService` (satır 43-44) master'a istek atarken JWT değil sadece `X-Master-Token` header'ı gönderiyor. `MasterTokenFilter` ise yalnızca `/api/v1/slave/**` path'i için kayıtlı.
- **Sonuç:** Slave'in `registerWithMaster()` ve `sendHeartbeat()` çağrıları master tarafında 403 Forbidden ile reddedilir. Otomatik slave enrollment ve heartbeat **sahada çalışmaz**.
- Testler (`MasterControllerTest`) `addFilters = false` kullandığı için bu sorunu yakalamamış.
- **Çözüm:** `/api/v1/master/slaves/**` endpoint'lerini MasterTokenFilter kapsamına almak veya ayrı bir inter-node auth mekanizması tasarlamak.

### CRIT-02 — SSRF + Master Token Sızdırma Riski (slaveAddress Kontrolsüz)

- **Dosyalar:** `MasterController.java` satır 35, `MasterService.java` satır 86-99
- `registerSlave()` endpoint'inde `slaveAddress` herhangi bir URL olabilir — doğrulama veya allowlist yok. `MasterService.pushConfigToAllSlaves()` bu adrese `X-Master-Token` header'ı ile HTTP isteği atıyor.
- **Sonuç:** Tenant admin kendi kontrol ettiği bir sunucuyu `slaveAddress` olarak kaydederse, master `X-Master-Token`'ı bu adrese gönderir → kontrol düzlemi sırrı sızdırılır. Klasik SSRF.
- **Çözüm:** Host allowlist, mutual TLS veya imzalı node enrollment mekanizması.

### CRIT-03 — Bootstrap SUPER_ADMIN Parolası Log'a Açık Metin Yazılıyor

- **Dosya:** `DataInitializer.java` satır 31
- `DEFAULT_ADMIN_PASSWORD` env var'ı yoksa random UUID üretilip `log.warn("... Generated random password: {}", password)` ile log'a yazılıyor.
- **Sonuç:** Log erişimi olan herkes (ELK, CloudWatch, Splunk vb.) platform SUPER_ADMIN hesabını ele geçirir. Doğrudan prod-blocker seviyesinde.
- **Çözüm:** Şifreyi log'a yazmak yerine özel bir stdout satırı kullanmak veya ilk login'de zorunlu şifre değişikliği mekanizması eklemek.

### CRIT-04 — Helm Chart Repo'da Gerçek Secret Değerler Taşıyor

- **Dosyalar:** `helm/values.yaml` satır 32-37, `helm/templates/secret.yaml`
- `values.yaml` içinde `MONGO_PASSWORD: changeme`, `ENCRYPTION_SECRET: changeme-32chars-minimum-key-here`, `JWT_SECRET: changeme-jwt-secret-hex-string-here` şeklinde placeholder'lar var. Template bunları sadece base64 encode ediyor.
- **Sonuç:** `helm install` sırasında override unutulursa bilinen değerlerle deployment yapılır.
- **Çözüm:** External secret manager (Vault, AWS Secrets Manager), Sealed Secrets veya en azından boş/defaultsuz zorunlu değerler.

### CRIT-05 — DTO Katmanı Yok: Domain Modeller Doğrudan Expose Ediliyor

- **Dosyalar:** Tüm `*Controller.java`, özellikle `SubscriberController.java`, `TenantController.java`
- `Subscriber`, `Tenant`, `License` gibi domain nesneleri request body'den doğrudan deserialize edilip response olarak serialize ediliyor.
- **Sonuçları:**
  - `imsiHash`, `msisdnHash` gibi iç kriptografik alanlar JSON response'a sızıyor (bkz. MED-06).
  - Model değişikliği = API değişikliği → sıkı coupling.
  - `@Version`, `@CreatedBy`, `createdAt` alanları istemci tarafından body'de gönderilebilir; `@Schema(accessMode = READ_ONLY)` sadece Swagger dokümantasyonunu etkiler, deserializasyonu engellemez.
- **Çözüm:** MapStruct ile `*Request/*Response` DTO çifti. Minimum geçici çözüm: `@JsonIgnore` ile hash alanlarını response'dan kapatmak.

### CRIT-06 — IMSI/MSISDN Hash'i Saltsız SHA-256 ile Yapılıyor

- **Dosya:** `EncryptionService.java` satır 80-93
- IMSI 15 haneli bir rakamdır — toplam 10^15 olası değer. Saldırgan salt olmadan tüm IMSI uzayı için rainbow table oluşturabilir (GPU ile saniyeler içinde).
- **Çözüm:** `HMAC-SHA256(IMSI, masterKey)` kullanılmalı — anahtar olmadan hash üretilemez. Bu değişiklik mevcut hash'leri geçersiz kılar, migration script gerekir.

### CRIT-07 — AES Anahtarı SHA-256 ile Türetiliyor, PBKDF2 Kullanmıyor

- **Dosya:** `EncryptionService.java` satır 42-44
- Master key bir string ise SHA-256 tek-geçişli türetme düşük entropili anahtarlarda brute force'a açık.
- **Çözüm:** `PBKDF2WithHmacSHA256` veya Argon2 ile key stretching.

### CRIT-08 — `getAllSubscribers()` Limitsiz Belleğe Yüklüyor

- **Dosya:** `SubscriberService.java` satır 343-347
- `findByTenantId(tenantId)` tüm koleksiyonu JVM heap'e çeker + her kayıt için AES decrypt yapar. Yüz binlerce subscriber'da OOM riski.

### CRIT-09 — `AlarmService.getAlarms()` Sınırsız Liste Dönüyor

- **Dosya:** `domain/observability/service/AlarmService.java` satır 105-114
- `List<Alarm>` dönüyor, `Page<Alarm>` değil. Yoğun alarm üreten bir tenant'ta bellek tükenmesine yol açabilir.

### CRIT-10 — `pm_metrics` Koleksiyonunda TTL Yok

- **Dosya:** `domain/performance/model/PmMetric.java`
- Metrik verileri sürekli birikir, MongoDB disk alanı sınırlı. `AuditLog`'da `expireAfterSeconds` var, `PmMetric`'te yok — uzun vadede disk dolumu.

### CRIT-11 — `PmService` Tüm Örnekleri Belleğe Çekiyor

- **Dosya:** `domain/performance/service/PmService.java` satır 62-67, 82-87
- `getTotalDataGB` ve `getCurrentThroughput` tüm zaman aralığındaki örnekleri belleğe yükleyip işliyor. Aggregation pipeline'a taşınmalı.

### CRIT-12 — `Open5gsSyncScheduler` N+1 Sorgu Pattern'i

- **Dosya:** `integration/open5gs/Open5gsSyncScheduler.java` satır 91-159, 186-215, 248-274
- Her gNB/UE/PDU için tek tek `find + save` yapılıyor. Çok sayıda tenant ve cihazda yavaşlama ve timeout riski.

### CRIT-13 — `SubscriberService.deleteSubscribersBatch` N+1

- **Dosya:** `domain/subscriber/service/SubscriberService.java` satır 148-164
- Her IMSI için ayrı `find` sorgusu. Toplu silmede `$in` sorgusu kullanılmalı.

---

## 3. Orta Öncelikli Sorunlar

### MED-01 — "Change Own Password" Endpoint'i Principal Kontrolü Yapmıyor

- **Dosyalar:** `SecurityConfiguration.java`, `UserController.java` satır 72-80, `UserService.java`
- `PUT /api/v1/users/{userId}/password` güvenlik kuralı sadece `authenticated()`. Controller path'teki `userId`'yi doğrudan servise iletiyor, kimliği doğrulanmış kullanıcıyla (principal) eşleştirmiyor.
- **Sonuç:** Aynı tenant'taki herhangi bir kullanıcı, hedef hesabın mevcut parolasını biliyorsa o hesabın parolasını değiştirebilir. Bu "change own password" değil, "change any password with knowledge" oluyor.
- **Çözüm:** Self-service akış principal'a bağlanmalı (JWT'deki `userId` == path `userId`); başka kullanıcı için sadece ADMIN reset akışı kalmalı (mevcut şifreyi bilmeyi gerektirmeyen `resetPassword`).

### MED-02 — `DashboardService` 12 DB Sorgusu + 3 Senkron HTTP Çağrısı

- **Dosya:** `domain/dashboard/service/DashboardService.java`
- Tek bir `getSummary()` çağrısında ~12 ayrı MongoDB sorgusu ve 3 ayrı senkron HTTP isteği (Open5GS NF listesi) yapılıyor. Herhangi bir timeout veya hata tüm dashboard'u devre dışı bırakır.
- **Çözüm:** Kısa süreli cache (30s-60s), paralel yürütme (`CompletableFuture`), veya circuit breaker.

### MED-03 — `Tenant.open5gsMongoUri` MongoDB'de Açık Metin Saklanıyor

- **Dosya:** `domain/tenant/model/Tenant.java`
- Her tenant'ın Open5GS MongoDB URI'ı (`mongodb://user:password@host:port/db`) şifrelenmeden saklanıyor. Veritabanı dump'ına erişen biri tüm Open5GS instance'larına bağlanabilir.
- **Çözüm:** `EncryptionService` ile şifreleme veya vault referansı.

### MED-04 — `AuditAspect` Kırılgan `tenantId` Çıkarımı

- **Dosya:** `domain/audit/aspect/AuditAspect.java`
- AOP aspect, `args[0]`'ın `String` olduğunu ve `tenantId` taşıdığını varsayıyor; bu konvansiyon bozulursa sessizce yanlış tenantId kaydeder veya `"SYSTEM"` yazar. Parametre sırası değişirse ya da yeni bir wrapper eklenirse audit log'ları tutarsızlaşır.
- **Çözüm:** `RequestContextHolder` veya `ThreadLocal` üzerinden almak.

### MED-05 — `SlaveController` Yetki Modeli Sadece `MasterTokenFilter`'a Dayanıyor

- **Dosyalar:** `SecurityConfiguration.java`, `MasterTokenFilterConfig.java`
- `/api/v1/slave/**` path'i `permitAll()` ile açılmış, koruma yalnızca servlet filter sıralamasına bağlı `MasterTokenFilter`'da. Filter sırası değişirse veya yeni bir filter chain eklenirse bu path tamamen açık kalır.
- **Çözüm:** `MasterTokenFilter`'ı Spring Security filter chain'ine dahil etmek (`addFilterBefore/After`).

### MED-06 — `imsiHash` / `msisdnHash` JSON Response'da Görünüyor

- **Dosyalar:** `domain/subscriber/model/Subscriber.java`, `SubscriberController.java`
- DTO katmanı olmadığı için (bkz. CRIT-05) kriptografik hash alanları API response'unda istemciye dönüyor. Bu alanlar iç deduplikasyon içindir, istemcinin görmesi gereksiz ve hash saldırı yüzeyini genişletir.
- **Geçici çözüm:** `@JsonIgnore` ile gizleme.

### MED-07 — 6 Controller İçin Test Yok

Aşağıdaki controller'ların hiçbirinde `*ControllerTest.java` yok:

- `PolicyController`
- `NetworkConfigController`
- `AmfConfigController`, `SmfConfigController`, `UpfConfigController`
- `Open5gsDeployController`

### MED-08 — 18 Controller Testi Güvenlik Zincirini Atlıyor

`addFilters = false` kullanan 18 test dosyası JWT/RBAC'ı test etmiyor. Sadece 4 security integration testi var ve bunlar sadece subscriber, user, firewall ve system-update endpoint'lerini kapsıyor.

### MED-09 — Exception Tipi Tutarsızlığı

| Servis | Exception Tipi |
|--------|---------------|
| Çoğunluk | `ResponseStatusException` |
| `MasterService` | `IllegalStateException`, `EntityNotFoundException` |
| `UpdateService` | `RuntimeException` |
| `IptablesExecutor` | `IllegalArgumentException` |

### MED-10 — `SubscriberService` Çok Fazla Sorumluluk Taşıyor

Tek bir serviste: CRUD + Open5GS provisioning + lisans kontrolü + policy + APN zenginleştirme + bulk import. En az Open5GS provisioning ve bulk import ayrı servislere alınmalı.

### MED-11 — `Inventory POST` Tüm Rollere Açık

- **Dosya:** `SecurityConfiguration.java` satır 138-139
- `/api/v1/inventory/**` sadece `authenticated()`. VIEWER rolü bile `POST /api/v1/inventory/nodes/resources` çağırabilir.

### MED-12 — `GlobalExceptionHandler` — Ham Mesaj Sızıntısı

- **Dosya:** `common/exception/GlobalExceptionHandler.java` satır 94-104, 143-177
- `AuthenticationException`, `EntityNotFoundException`, `IllegalArgumentException` handler'ları `ex.getMessage()` değerini direkt istemciye dönüyor — iç detaylar sızabilir.

### MED-13 — Subscriber MSISDN Sorgusu İçin Index Eksik

- **Dosya:** `domain/subscriber/model/Subscriber.java`
- `findByMsisdnHashAndTenantId` için compound index tanımlı değil. Sadece `tenant_imsi_hash_idx` var.

### MED-14 — `Tenant.active` Alanı İndexlenmemiş

- **Dosya:** `domain/tenant/model/Tenant.java` satır 34-35
- `findByActiveTrue()` sorgusu collection scan yapar.

### MED-15 — `SlaveClientService.sendHeartbeat` ShedLock Yok

- **Dosya:** `domain/multitenant/service/SlaveClientService.java`
- Yatay ölçeklenmede aynı heartbeat birden fazla instance'dan gönderilir.

---

## 4. Düşük Öncelikli İyileştirmeler

### LOW-01 — Endpoint İsimlendirme Tutarsızlığı

`/api/v1/subscriber` (tekil) vs `/api/v1/users`, `/api/v1/policies`, `/api/v1/certificates` (çoğul). Subscriber tek istisna.

### LOW-02 — Türkçe Yorumlar

~20+ dosyada Türkçe yorum var. Kod tabanı İngilizce'ye standardize edilmeli.

### LOW-03 — Dependency Güncellemeleri

| Dependency | Mevcut | Güncel |
|-----------|--------|--------|
| `springdoc-openapi` | 2.3.0 | 2.8.x |
| `fabric8 kubernetes-client` | 6.10.0 | 7.x (breaking) |
| Spring Boot | 3.2.3 | 3.2.x patch |

### LOW-04 — Prometheus Registry Eksik

`pom.xml`'de `micrometer-registry-prometheus` dependency'si yok. Backlog'da Prometheus entegrasyonu "tamamlandı" yazıyor ama bu sadece Open5GS metric scraping, uygulamanın kendi metrikleri `/actuator/prometheus`'tan sunulmuyor.

### LOW-05 — `application.yml` DEBUG Loglama

`com.opticoms.optinmscore: DEBUG` ve MongoDB `DEBUG` — dev için uygun ama paylaşımlı ortamlarda sorgu detayları sızabilir. Prod profili bunu WARN'a çeviriyor (doğru).

### LOW-06 — `DashboardControllerTest` Sadece 1 Test İçeriyor

Dashboard servisinin 6 farklı hesaplama yaptığı düşünülürse çok yetersiz.

### LOW-07 — `SecurityConfiguration`'da Gereksiz Kural

`/api/v1/system/tenants/**` (SUPER_ADMIN) ile `/api/v1/system/**` (SUPER_ADMIN) — ilk kural ikincinin altında zaten kapsanır, gereksiz ama zararsız.

### LOW-08 — `ResourceNotFoundException` ve `EntityNotFoundException` Çakışması

İki farklı not-found exception sınıfı aynı semantik için kullanılıyor. Tek birine standardize edilmeli.

### LOW-09 — Dockerfile JVM Bellek Flag'leri Eksik

`-XX:+UseContainerSupport` ve `-XX:MaxRAMPercentage` gibi container-aware JVM flag'leri tanımlı değil. Kubernetes'te OOMKill riski.

### LOW-10 — CI/CD Pipeline'da OWASP Dependency Check Yok

GitHub Actions workflow sadece test + build + Docker push yapıyor. Bilinen CVE'lere karşı dependency taraması yok.

### LOW-11 — ShedLock `lockAtMostFor` Boşluğu

`Open5gsSyncScheduler` 30 saniyelik schedule ile çalışıyor. Eğer `lockAtMostFor` schedule aralığından kısa tutulursa iki instance aynı sync'i çalıştırabilir.

### LOW-12 — `InMemoryRateLimiter` Multi-Instance'da Çalışmaz

Rate limiter JVM-local `ConcurrentHashMap` kullanıyor. Birden fazla pod varsa her biri kendi sayacını tutar, efektif limit N katına çıkar.

---

## 5. Öncelikli Refactor Gerektiren Dosyalar/Modüller

| Öncelik | Dosya(lar) | Neden |
|---------|-----------|-------|
| 1 | `MasterController.java`, `MasterService.java`, `MasterTokenFilterConfig.java`, `SecurityConfiguration.java` | Master/Slave auth çelişkisi + SSRF (CRIT-01, CRIT-02) |
| 2 | `DataInitializer.java` | SUPER_ADMIN parolası log'a yazılıyor (CRIT-03) |
| 3 | `helm/values.yaml`, `helm/templates/secret.yaml` | Secret'lar repo'da (CRIT-04) |
| 4 | `EncryptionService.java` | SHA-256 → HMAC-SHA256 hash + PBKDF2 key derivation (CRIT-06, CRIT-07) |
| 5 | Tüm Controller'lar + yeni DTO paketi | DTO katmanı eklenmeli (CRIT-05) |
| 6 | `SubscriberService.java` | Çok fazla sorumluluk + `getAllSubscribers()` limitsiz (CRIT-08, MED-10) |
| 7 | `AlarmService.java` | `getAlarms()` → `Page<Alarm>` dönüşüm (CRIT-09) |
| 8 | `PmService.java` | Bellek içinde aggregation → MongoDB aggregation pipeline (CRIT-11) |
| 9 | `Open5gsSyncScheduler.java` | N+1 → batch upsert (CRIT-12) |
| 10 | `UserController.java`, `SecurityConfiguration.java` | Password endpoint principal kontrolü (MED-01) |
| 11 | `DashboardService.java` | Senkron I/O + cache yok (MED-02) |
| 12 | `GlobalExceptionHandler.java` | Ham exception mesajları → curated mesajlar (MED-12) |
| 13 | `PmMetric.java` | TTL index eklenmeli (CRIT-10) |

---

## 6. Somut Öneriler (Dosya Referanslı)

### Güvenlik — Acil

**6.1.** `MasterTokenFilterConfig.java` → `/api/v1/master/slaves/**` path'ini de `MasterTokenFilter` kapsamına al. `SecurityConfiguration.java`'da bu path'i JWT zincirinden muaf tut (`permitAll` + filter koruma). Slave register/heartbeat istekleri JWT değil sadece `X-Master-Token` kullanmalı.

**6.2.** `MasterService.registerSlave()` → `slaveAddress` için URL validation + host allowlist ekle. `pushConfigToAllSlaves()` çağrısından önce adres doğrulanmalı. Ideal: mutual TLS veya imzalı enrollment token.

**6.3.** `DataInitializer.java` satır 31 → `log.warn("Generated random password: {}")` satırını kaldır. Alternatif: ilk login'de zorunlu şifre değiştirme mekanizması (DB'de `mustChangePassword` flag'i).

**6.4.** `helm/values.yaml` → Default secret değerleri (`changeme`, `changeme-32chars...`) kaldır, boş bırak veya fail-fast validation ekle. Sealed Secrets veya External Secrets Operator kullanımına geç.

**6.5.** `EncryptionService.java` → SHA-256 hash yerine `HMAC-SHA256(data, masterKey)` kullan. Key derivation için `PBKDF2WithHmacSHA256` (100k+ iteration) uygula. Migration script gerekir.

**6.6.** `UserController.changePassword()` → JWT'deki `userId` == path `userId` kontrolü ekle. Başka kullanıcı için sadece `resetPassword` (ADMIN-only, mevcut şifreyi bilmeyi gerektirmeyen) akışı kalmalı.

### Veri Koruma

**6.7.** DTO katmanı ekle: en azından `SubscriberResponse`, `TenantResponse`, `LicenseResponse` oluştur. `imsiHash`, `msisdnHash`, `@Version` gibi iç alanlar response'dan çıkarılsın. Geçici çözüm: `@JsonIgnore`.

**6.8.** `Tenant.open5gsMongoUri` → `EncryptionService.encrypt()` ile şifrelenerek saklanmalı, kullanıldığında decrypt edilmeli.

### Performans

**6.9.** `AlarmService.getAlarms()` (satır 105-114) → Parametre olarak `Pageable` alsın, `Page<Alarm>` dönsün.

**6.10.** `PmMetric.java`'ya TTL index ekle:

```java
@Indexed(expireAfterSeconds = 2592000) // 30 gün
private Date timestamp;
```

**6.11.** `PmService.getTotalDataGB()` ve `getCurrentThroughput()` → MongoDB aggregation (`$match` + `$group` + `$sum`) ile değiştir.

**6.12.** `Open5gsSyncScheduler` → `bulkWrite` / `upsert` pattern'ine geç. `SubscriberService.deleteSubscribersBatch` için de `$in` sorgusu.

**6.13.** `SubscriberService.getAllSubscribers()` → `Page<Subscriber>` dönsün veya akış tabanlı (`Stream<Subscriber>`) olsun.

**6.14.** `DashboardService.getSummary()` → 30-60 saniyelik cache (`@Cacheable`), senkron HTTP çağrıları için `CompletableFuture.allOf()` veya circuit breaker.

### Index ve Veritabanı

**6.15.** `Subscriber.java`'ya MSISDN compound index ekle:

```java
@CompoundIndex(name = "tenant_msisdn_hash_idx", def = "{'tenantId': 1, 'msisdnHash': 1}")
```

**6.16.** `Tenant.java`'ya `active` alanı için index ekle (az sayıda tenant olsa bile sorgu consistency için).

### RBAC ve Erişim Kontrolü

**6.17.** `SecurityConfiguration.java` satır 138-139 → Inventory POST kısıtlaması:

```java
.requestMatchers(HttpMethod.POST, "/api/v1/inventory/**").hasAnyRole("ADMIN", "OPERATOR")
.requestMatchers(HttpMethod.GET, "/api/v1/inventory/**").authenticated()
```

**6.18.** `MasterTokenFilter`'ı Spring Security filter chain'ine dahil et (`addFilterBefore(UsernamePasswordAuthenticationFilter.class)`) — servlet filter sıralamasına bağımlılık kaldırılsın.

### Hata Yönetimi ve Gözlemlenebilirlik

**6.19.** `GlobalExceptionHandler` — `ex.getMessage()` yerine sabit mesajlar kullan, detaylı mesajlar sadece log'a.

**6.20.** Tüm servislerde `ResponseStatusException`'a standardize ol — `IllegalStateException`, `RuntimeException` atılan yerler değiştirilmeli.

### Test

**6.21.** Eksik 6 controller için `@WebMvcTest` test dosyaları oluştur: `PolicyControllerTest`, `NetworkConfigControllerTest`, `AmfConfigControllerTest`, `SmfConfigControllerTest`, `UpfConfigControllerTest`, `Open5gsDeployControllerTest`.

**6.22.** `addFilters = false` kullanan testlerin yanına `addFilters = true` security integration testleri ekle.

### Kod Organizasyonu

**6.23.** `SubscriberService` → `BulkImportService` ve `SubscriberProvisioningService` olarak böl.

### Operasyonel

**6.24.** Dockerfile'a container-aware JVM flag'leri ekle: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`.

**6.25.** CI/CD pipeline'a OWASP dependency-check-maven eklentisi ekle.

---

## 7. Bulgu Özet Tablosu

| ID | Seviye | Konu | Dosya(lar) |
|----|--------|------|-----------|
| CRIT-01 | Kritik | Master/Slave auth çelişkisi (sahada çalışmaz) | SecurityConfiguration, MasterController, SlaveClientService, MasterTokenFilterConfig |
| CRIT-02 | Kritik | SSRF + masterToken sızdırma (slaveAddress) | MasterController, MasterService |
| CRIT-03 | Kritik | SUPER_ADMIN parolası log'a açık metin | DataInitializer |
| CRIT-04 | Kritik | Helm chart'ta gerçek secret değerler | helm/values.yaml, helm/templates/secret.yaml |
| CRIT-05 | Kritik | DTO katmanı yok, domain modeller expose | Tüm Controller'lar |
| CRIT-06 | Kritik | Saltsız SHA-256 IMSI/MSISDN hash | EncryptionService |
| CRIT-07 | Kritik | SHA-256 key derivation (PBKDF2 yok) | EncryptionService |
| CRIT-08 | Kritik | getAllSubscribers() limitsiz bellek yükü | SubscriberService |
| CRIT-09 | Kritik | getAlarms() sınırsız liste | AlarmService |
| CRIT-10 | Kritik | pm_metrics TTL yok | PmMetric |
| CRIT-11 | Kritik | PmService tüm metrikleri belleğe çekiyor | PmService |
| CRIT-12 | Kritik | Open5gsSyncScheduler N+1 | Open5gsSyncScheduler |
| CRIT-13 | Kritik | deleteSubscribersBatch N+1 | SubscriberService |
| MED-01 | Orta | changePassword principal kontrolü yok | UserController, SecurityConfiguration |
| MED-02 | Orta | DashboardService 12 DB + 3 HTTP senkron | DashboardService |
| MED-03 | Orta | open5gsMongoUri açık metin | Tenant |
| MED-04 | Orta | AuditAspect kırılgan tenantId çıkarımı | AuditAspect |
| MED-05 | Orta | SlaveController sadece filter'a dayalı | SecurityConfiguration, MasterTokenFilterConfig |
| MED-06 | Orta | imsiHash/msisdnHash response'da görünüyor | Subscriber, SubscriberController |
| MED-07 | Orta | 6 controller testi yok | Policy, NetworkConfig, Amf/Smf/Upf, Deploy |
| MED-08 | Orta | 18 test güvenlik zincirini atlıyor | 18 *ControllerTest dosyası |
| MED-09 | Orta | Exception tipi tutarsızlığı | MasterService, UpdateService, IptablesExecutor |
| MED-10 | Orta | SubscriberService aşırı sorumluluk | SubscriberService |
| MED-11 | Orta | Inventory POST tüm rollere açık | SecurityConfiguration |
| MED-12 | Orta | GlobalExceptionHandler mesaj sızıntısı | GlobalExceptionHandler |
| MED-13 | Orta | MSISDN compound index eksik | Subscriber |
| MED-14 | Orta | Tenant.active index yok | Tenant |
| MED-15 | Orta | SlaveClient heartbeat ShedLock yok | SlaveClientService |
| LOW-01 | Düşük | Endpoint isimlendirme tutarsızlığı | — |
| LOW-02 | Düşük | Türkçe yorumlar | 20+ dosya |
| LOW-03 | Düşük | Dependency güncellemeleri | pom.xml |
| LOW-04 | Düşük | Prometheus registry eksik | pom.xml |
| LOW-05 | Düşük | DEBUG loglama dev profilde | application.yml |
| LOW-06 | Düşük | DashboardControllerTest 1 test | DashboardControllerTest |
| LOW-07 | Düşük | SecurityConfig gereksiz kural | SecurityConfiguration |
| LOW-08 | Düşük | ResourceNotFoundException/EntityNotFoundException çakışma | — |
| LOW-09 | Düşük | Dockerfile JVM flag eksik | Dockerfile |
| LOW-10 | Düşük | CI/CD OWASP check yok | GitHub Actions |
| LOW-11 | Düşük | ShedLock lockAtMostFor boşluğu | Open5gsSyncScheduler |
| LOW-12 | Düşük | InMemoryRateLimiter multi-instance | RateLimiter |

**Toplam: 13 Kritik, 15 Orta, 12 Düşük**

---

## 8. Planlı İyileştirme: Login Akışı (TenantId Gömme)

### Mevcut Durum

Login isteği 3 alan gönderiyor:

```json
{ "tenantId": "TURK-0001/0001/01", "username": "admin", "password": "..." }
```

Kullanıcının `tenantId`'yi ayrı bir alan olarak bilmesi ve girmesi gerekiyor — kötü UX.

### Hedef

Kullanıcı tek bir `username` alanına `admin@TURK-0001/0001/01` formatında giriş yapar. Backend `@` ayırıcıdan tenantId'yi parse eder.

### Değişiklik Planı

| Dosya | Değişiklik |
|-------|-----------|
| `AuthController.java` | `LoginRequest.tenantId` alanı kaldırılır. `login()` içinde `username` `@` ile parse edilir + guard eklenir |
| `AuthControllerTest.java` | 5 testin tamamı `{"username":"admin@TURK-0001/0001/01", ...}` formatına geçer |

**Değişmeyen dosyalar:** `JwtService`, `CustomUserDetailsService`, `UserRepository`, `SecurityConfiguration`, `User` modeli — hiçbiri etkilenmez. Parse sonrası `loadUserByUsernameAndTenantId(username, tenantId)` çağrısı aynı kalır, JWT'deki `tenantId` claim'i aynı şekilde üretilir.

### Uygulama Detayları

**1. `AuthController.login()` — Parse + Guard:**

```java
String[] parts = request.getUsername().split("@", 2);
if (parts.length != 2 || parts[1].isBlank()) {
    throw new BadCredentialsException("Invalid credentials");
    // Format bilgisi sızdırılmaz — saldırgan username formatını keşfetmesin
}
String username = parts[0];
String tenantId = parts[1];
```

**2. `LoginRequest` DTO:**

```java
@Data
@NoArgsConstructor
public static class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;  // format: user@tenantId
    @NotBlank(message = "Password is required")
    private String password;
}
```

**3. Test senaryoları:**

- `login_success` → `username: "admin@TURK-0001/0001/01"`
- `login_wrongPassword` → aynı format, yanlış şifre
- `login_missingTenantId` → artık `username: "admin"` (@ yok) → `BadCredentialsException`
- `login_disabledUser` → aynı format, devre dışı kullanıcı
- `login_missingUsername` → boş username

### Notlar

- Ayırıcı `@` güvenli: tenant ID formatı `TURK-0001/0001/01` (slash içerir ama `@` yok).
- Swagger'da `username` alanına `@Schema(description = "Format: username@tenantId", example = "admin@TURK-0001/0001/01")` eklenmeli.
- Hata mesajında format bilgisi verilmiyor (`"Invalid credentials"`) — bu bilinçli bir güvenlik kararı.

---

## 9. Nihai Değerlendirme

**KABUL EDİLEBİLİR (Acceptable)** — ancak güvenlik bulguları kapatılmadan production'a geçirilmemeli.

Proje, MVP ve erken production aşaması için **iyi yapılmış bir monolitik backend**. Modül yapısı temiz, multi-tenancy doğru uygulanmış, test coverage makul (%60+ JaCoCo), CI/CD pipeline mevcut. Ancak:

- **Master/Slave sistemi** mevcut haliyle sahada çalışmıyor (CRIT-01) ve SSRF'e açık (CRIT-02) — production geçiş öncesi düzeltilmeli veya devre dışı bırakılmalı.
- **Kriptografik altyapı** (CRIT-06, CRIT-07) endüstri standardının altında — kısa vadede HMAC-SHA256 geçişi, orta vadede PBKDF2 key stretching.
- **Operasyonel güvenlik** (CRIT-03, CRIT-04) acil düzeltme gerektirir — prod-blocker seviyesinde.
- **Performans sorunları** (CRIT-08 ~ CRIT-13) tenant başına veri hacmi arttıkça OOM/timeout olarak kendini gösterecek.

| Kriter | Puan (1-5) | Not |
|--------|-----------|-----|
| Mimari ve yapı | 4 | Feature-based paketleme tutarlı |
| Kod organizasyonu | 3 | SubscriberService aşırı büyük, DTO yok |
| İsimlendirme tutarlılığı | 3.5 | Subscriber tekil/çoğul karışık |
| Tip güvenliği | 3.5 | Domain model doğrudan expose |
| Hata yönetimi | 3 | Exception tutarsızlığı, ham mesaj sızıntısı |
| Güvenlik | 2.5 | Master/Slave auth, SSRF, saltsız hash, log'da parola |
| Test kapsamı | 3 | 6 controller testi yok, 18 test güvenlik atlıyor |
| Performans | 2.5 | Sınırsız listeler, N+1, senkron dashboard |
| CI/CD hazırlık | 3.5 | OWASP check ve JVM flag eksik |
| Bakım kolaylığı | 3 | Büyük servisler, exception karışıklığı |
| Endüstri standartları | 3 | PBKDF2 yok, DTO yok, allowlist yok |
| **Genel** | **3.1 / 5** | Güvenlik bulguları düzeltilirse 3.8'e çıkabilir |
