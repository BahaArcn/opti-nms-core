package com.opticoms.optinmscore.domain.subscriber.service;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.subscriber.repository.SubscriberRepository;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.repository.TenantRepository;
import com.opticoms.optinmscore.integration.open5gs.Open5gsProvisioningService;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriberServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String IMSI = "286010000000001";
    private static final String IMSI_HASH = "hashed-imsi";
    private static final String KI = "465B5CE8B199B49FAA5F0A2EE238A6BC";
    private static final String OPC = "E8ED289DEBA952E4283B54E88E6183CA";
    private static final String OPEN5GS_URI = "mongodb://localhost:27018/open5gs";

    @Mock private SubscriberRepository subscriberRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private Open5gsProvisioningService open5gsProvisioning;
    @Mock private TenantRepository tenantRepository;

    @InjectMocks
    private SubscriberService service;

    private Subscriber subscriber;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        subscriber = buildSubscriber();
        testTenant = new Tenant();
        testTenant.setTenantId(TENANT);
        testTenant.setName("Test Tenant");
        testTenant.setAmfUrl("http://localhost:9090");
        testTenant.setSmfUrl("http://localhost:9091");
        testTenant.setOpen5gsMongoUri(OPEN5GS_URI);
    }

    @Test
    void createSubscriber_success() {
        when(encryptionService.hash(anyString())).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        when(subscriberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(testTenant));

        Subscriber result = service.createSubscriber(TENANT, subscriber);

        assertEquals(TENANT, result.getTenantId());
        verify(open5gsProvisioning).provisionSubscriber(any(), eq(OPEN5GS_URI));
        verify(subscriberRepository).save(any());
    }

    @Test
    void createSubscriber_duplicateImsi_throwsConflict() {
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createSubscriber(TENANT, subscriber));

        assertEquals(409, ex.getStatusCode().value());
        verify(subscriberRepository, never()).save(any());
    }

    @Test
    void createSubscriber_invalidKi_throws() {
        subscriber.setKi("SHORT");
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> service.createSubscriber(TENANT, subscriber));
    }

    @Test
    void createSubscriber_invalidHexKi_throws() {
        subscriber.setKi("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ");
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> service.createSubscriber(TENANT, subscriber));
    }

    @Test
    void createSubscriber_provisioningFails_throwsServiceUnavailable() {
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(testTenant));
        doThrow(new RuntimeException("Open5GS down"))
                .when(open5gsProvisioning).provisionSubscriber(any(), anyString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createSubscriber(TENANT, subscriber));

        assertEquals(503, ex.getStatusCode().value());
    }

    @Test
    void createSubscriber_encryptsSensitiveFieldsBeforeSave() {
        when(encryptionService.hash(anyString())).thenReturn("hash-value");
        when(subscriberRepository.existsByTenantIdAndImsiHash(eq(TENANT), anyString())).thenReturn(false);
        when(encryptionService.encrypt(IMSI)).thenReturn("enc-imsi");
        when(encryptionService.encrypt(KI)).thenReturn("enc-ki");
        when(encryptionService.encrypt(OPC)).thenReturn("enc-opc");
        when(subscriberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(testTenant));

        service.createSubscriber(TENANT, subscriber);

        ArgumentCaptor<Subscriber> captor = ArgumentCaptor.forClass(Subscriber.class);
        verify(subscriberRepository).save(captor.capture());

        Subscriber saved = captor.getValue();
        assertEquals("enc-imsi", saved.getImsi(), "IMSI should be encrypted");
        assertEquals("enc-ki", saved.getKi(), "Ki should be encrypted");
        assertEquals("enc-opc", saved.getOpc(), "OPc should be encrypted");
        assertNotNull(saved.getImsiHash(), "IMSI hash should be set");
    }

    @Test
    void getSubscriber_found_decrypts() {
        Subscriber encrypted = buildSubscriber();
        encrypted.setImsi("enc-imsi");
        encrypted.setKi("enc-ki");
        encrypted.setOpc("enc-opc");

        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.findByImsiHashAndTenantId(IMSI_HASH, TENANT))
                .thenReturn(Optional.of(encrypted));
        when(encryptionService.decrypt("enc-imsi")).thenReturn(IMSI);
        when(encryptionService.decrypt("enc-ki")).thenReturn(KI);
        when(encryptionService.decrypt("enc-opc")).thenReturn(OPC);

        Subscriber result = service.getSubscriber(TENANT, IMSI);

        assertEquals(IMSI, result.getImsi(), "IMSI should be decrypted");
        assertEquals(KI, result.getKi(), "Ki should be decrypted");
        assertEquals(OPC, result.getOpc(), "OPc should be decrypted");
    }

    @Test
    void getSubscriber_notFound_throws404() {
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.findByImsiHashAndTenantId(IMSI_HASH, TENANT))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getSubscriber(TENANT, IMSI));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void deleteSubscriber_success() {
        Subscriber existing = buildSubscriber();
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.findByImsiHashAndTenantId(IMSI_HASH, TENANT))
                .thenReturn(Optional.of(existing));
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(testTenant));

        service.deleteSubscriber(TENANT, IMSI);

        verify(open5gsProvisioning).deleteSubscriber(IMSI, OPEN5GS_URI);
        verify(subscriberRepository).delete(existing);
    }

    @Test
    void deleteSubscriber_open5gsFails_stillDeletesFromLocalDb() {
        Subscriber existing = buildSubscriber();
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.findByImsiHashAndTenantId(IMSI_HASH, TENANT))
                .thenReturn(Optional.of(existing));
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(testTenant));
        doThrow(new RuntimeException("connection refused"))
                .when(open5gsProvisioning).deleteSubscriber(IMSI, OPEN5GS_URI);

        service.deleteSubscriber(TENANT, IMSI);

        verify(subscriberRepository).delete(existing);
    }

    @Test
    void deleteSubscriber_notFound_throws404() {
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.findByImsiHashAndTenantId(IMSI_HASH, TENANT))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> service.deleteSubscriber(TENANT, IMSI));
    }

    @Test
    void deleteSubscribersBatch_deletesMultiple() {
        Subscriber sub1 = buildSubscriber();
        Subscriber sub2 = buildSubscriber();
        sub2.setImsi("286010000000002");

        when(encryptionService.hash("286010000000001")).thenReturn("h1");
        when(encryptionService.hash("286010000000002")).thenReturn("h2");
        when(subscriberRepository.findByImsiHashAndTenantId("h1", TENANT)).thenReturn(Optional.of(sub1));
        when(subscriberRepository.findByImsiHashAndTenantId("h2", TENANT)).thenReturn(Optional.of(sub2));
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(testTenant));

        int deleted = service.deleteSubscribersBatch(TENANT,
                List.of("286010000000001", "286010000000002"));

        assertEquals(2, deleted);
        verify(subscriberRepository, times(2)).delete(any());
    }

    @Test
    void getSubscriberCount_delegatesToRepo() {
        when(subscriberRepository.countByTenantId(TENANT)).thenReturn(42L);
        assertEquals(42L, service.getSubscriberCount(TENANT));
    }

    @Test
    void createSubscriber_tenantWithNoOpen5gsUri_provisionSkipped() {
        Tenant noUriTenant = new Tenant();
        noUriTenant.setTenantId(TENANT);
        noUriTenant.setOpen5gsMongoUri(null);

        when(encryptionService.hash(anyString())).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        when(subscriberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(noUriTenant));

        Subscriber result = service.createSubscriber(TENANT, subscriber);

        assertEquals(TENANT, result.getTenantId());
        verify(open5gsProvisioning).provisionSubscriber(any(), isNull());
        verify(subscriberRepository).save(any());
    }

    @Test
    void createSubscriber_nmsSaveFails_rollsBackOpen5gs() {
        when(encryptionService.hash(anyString())).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(testTenant));
        when(subscriberRepository.save(any())).thenThrow(new RuntimeException("DB write failed"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createSubscriber(TENANT, subscriber));

        assertEquals(503, ex.getStatusCode().value());
        verify(open5gsProvisioning).provisionSubscriber(any(), eq(OPEN5GS_URI));
        verify(open5gsProvisioning).deleteSubscriber(eq(IMSI), eq(OPEN5GS_URI));
    }

    @Test
    void updateSubscriber_nmsSaveFails_revertsOpen5gs() {
        Subscriber existing = buildSubscriber();
        existing.setImsi("enc-imsi");
        existing.setKi("enc-ki");
        existing.setOpc("enc-opc");
        existing.setImsiHash(IMSI_HASH);

        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.findByImsiHashAndTenantId(IMSI_HASH, TENANT))
                .thenReturn(Optional.of(existing));
        when(encryptionService.decrypt("enc-imsi")).thenReturn(IMSI);
        when(encryptionService.decrypt("enc-ki")).thenReturn(KI);
        when(encryptionService.decrypt("enc-opc")).thenReturn(OPC);
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        when(tenantRepository.findByTenantId(TENANT)).thenReturn(Optional.of(testTenant));
        when(subscriberRepository.save(any())).thenThrow(new RuntimeException("DB write failed"));

        Subscriber updatedData = buildSubscriber();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateSubscriber(TENANT, IMSI, updatedData));

        assertEquals(503, ex.getStatusCode().value());
        verify(open5gsProvisioning, times(2)).provisionSubscriber(any(), eq(OPEN5GS_URI));
    }

    // --- Helper ---

    private Subscriber buildSubscriber() {
        Subscriber s = new Subscriber();
        s.setImsi(IMSI);
        s.setKi(KI);
        s.setUsimType(Subscriber.UsimType.OPC);
        s.setOpc(OPC);
        s.setUeAmbrDl(1_000_000_000L);
        s.setUeAmbrUl(500_000_000L);

        Subscriber.SessionProfile profile = new Subscriber.SessionProfile();
        profile.setSst(1);
        profile.setApnDnn("internet");
        profile.setQci4g(9);
        profile.setQi5g(9);
        profile.setArpPriority(8);
        profile.setSessionAmbrDl(500_000_000L);
        profile.setSessionAmbrUl(250_000_000L);
        s.setProfileList(List.of(profile));

        return s;
    }
}
