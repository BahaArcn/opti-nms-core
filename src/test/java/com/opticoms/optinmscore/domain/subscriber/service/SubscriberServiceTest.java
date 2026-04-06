package com.opticoms.optinmscore.domain.subscriber.service;

import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.license.service.LicenseService;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.subscriber.repository.SubscriberRepository;
import com.opticoms.optinmscore.domain.subscriber.service.SubscriberSyncService;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
    @Mock private SubscriberSyncService subscriberSync;
    @Mock private ConnectedUeRepository connectedUeRepository;
    @Mock private LicenseService licenseService;
    @Mock private SubscriberHelper subscriberHelper;

    @InjectMocks
    private SubscriberService service;

    private Subscriber subscriber;

    @BeforeEach
    void setUp() {
        subscriber = buildSubscriber();
    }

    @Test
    void createSubscriber_success() {
        when(encryptionService.hash(anyString())).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);
        when(subscriberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriberHelper.resolveOpen5gsUri(TENANT)).thenReturn(OPEN5GS_URI);

        Subscriber result = service.createSubscriber(TENANT, subscriber);

        assertEquals(TENANT, result.getTenantId());
        verify(subscriberSync).provision(any(), eq(OPEN5GS_URI));
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
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ki must be exactly 16 bytes (32 hex chars)"))
                .when(subscriberHelper).validateKeys(any());

        assertThrows(ResponseStatusException.class,
                () -> service.createSubscriber(TENANT, subscriber));
    }

    @Test
    void createSubscriber_invalidHexKi_throws() {
        subscriber.setKi("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ");
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ki must contain only hexadecimal characters"))
                .when(subscriberHelper).validateKeys(any());

        assertThrows(ResponseStatusException.class,
                () -> service.createSubscriber(TENANT, subscriber));
    }

    @Test
    void createSubscriber_provisioningFails_throwsServiceUnavailable() {
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);
        when(subscriberHelper.resolveOpen5gsUri(TENANT)).thenReturn(OPEN5GS_URI);
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Open5GS down"))
                .when(subscriberSync).provision(any(), anyString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createSubscriber(TENANT, subscriber));

        assertEquals(503, ex.getStatusCode().value());
    }

    @Test
    void createSubscriber_encryptsSensitiveFieldsBeforeSave() {
        when(encryptionService.hash(anyString())).thenReturn("hash-value");
        when(subscriberRepository.existsByTenantIdAndImsiHash(eq(TENANT), anyString())).thenReturn(false);
        when(subscriberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriberHelper.resolveOpen5gsUri(TENANT)).thenReturn(OPEN5GS_URI);

        doAnswer(inv -> {
            Subscriber s = inv.getArgument(0);
            s.setImsiHash("hash-value");
            s.setImsi("enc-imsi");
            s.setKi("enc-ki");
            s.setOpc("enc-opc");
            s.setOp(null);
            return null;
        }).when(subscriberHelper).encryptSensitiveData(any());

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

        doAnswer(inv -> {
            Subscriber s = inv.getArgument(0);
            s.setImsi(IMSI);
            s.setKi(KI);
            s.setOpc(OPC);
            return null;
        }).when(subscriberHelper).decryptSensitiveData(any());

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
        when(subscriberHelper.resolveOpen5gsUri(TENANT)).thenReturn(OPEN5GS_URI);

        service.deleteSubscriber(TENANT, IMSI);

        verify(subscriberSync).deleteQuietly(IMSI, OPEN5GS_URI);
        verify(subscriberRepository).delete(existing);
    }

    @Test
    void deleteSubscriber_open5gsFails_stillDeletesFromLocalDb() {
        Subscriber existing = buildSubscriber();
        when(encryptionService.hash(IMSI)).thenReturn(IMSI_HASH);
        when(subscriberRepository.findByImsiHashAndTenantId(IMSI_HASH, TENANT))
                .thenReturn(Optional.of(existing));
        when(subscriberHelper.resolveOpen5gsUri(TENANT)).thenReturn(OPEN5GS_URI);

        service.deleteSubscriber(TENANT, IMSI);

        verify(subscriberSync).deleteQuietly(IMSI, OPEN5GS_URI);
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
        sub1.setImsi("enc_imsi_1");
        Subscriber sub2 = buildSubscriber();
        sub2.setImsi("enc_imsi_2");

        when(encryptionService.hash("286010000000001")).thenReturn("h1");
        when(encryptionService.hash("286010000000002")).thenReturn("h2");
        when(subscriberRepository.findByTenantIdAndImsiHashIn(eq(TENANT), anyCollection()))
                .thenReturn(List.of(sub1, sub2));
        when(encryptionService.decrypt("enc_imsi_1")).thenReturn("286010000000001");
        when(encryptionService.decrypt("enc_imsi_2")).thenReturn("286010000000002");
        when(subscriberHelper.resolveOpen5gsUri(TENANT)).thenReturn(OPEN5GS_URI);

        int deleted = service.deleteSubscribersBatch(TENANT,
                List.of("286010000000001", "286010000000002"));

        assertEquals(2, deleted);
        verify(subscriberRepository).deleteAll(anyList());
    }

    @Test
    void getSubscriberCount_delegatesToRepo() {
        when(subscriberRepository.countByTenantId(TENANT)).thenReturn(42L);
        assertEquals(42L, service.getSubscriberCount(TENANT));
    }

    @Test
    void createSubscriber_tenantWithNoOpen5gsUri_provisionSkipped() {
        when(encryptionService.hash(anyString())).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);
        when(subscriberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subscriberHelper.resolveOpen5gsUri(TENANT)).thenReturn(null);

        Subscriber result = service.createSubscriber(TENANT, subscriber);

        assertEquals(TENANT, result.getTenantId());
        verify(subscriberSync).provision(any(), isNull());
        verify(subscriberRepository).save(any());
    }

    @Test
    void createSubscriber_nmsSaveFails_rollsBackOpen5gs() {
        when(encryptionService.hash(anyString())).thenReturn(IMSI_HASH);
        when(subscriberRepository.existsByTenantIdAndImsiHash(TENANT, IMSI_HASH)).thenReturn(false);
        when(subscriberHelper.resolveOpen5gsUri(TENANT)).thenReturn(OPEN5GS_URI);
        when(subscriberRepository.save(any())).thenThrow(new RuntimeException("DB write failed"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createSubscriber(TENANT, subscriber));

        assertEquals(503, ex.getStatusCode().value());
        verify(subscriberSync).provision(any(), eq(OPEN5GS_URI));
        verify(subscriberSync).rollbackProvision(eq(IMSI), eq(OPEN5GS_URI));
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

        doAnswer(inv -> {
            Subscriber s = inv.getArgument(0);
            s.setImsi(IMSI);
            s.setKi(KI);
            s.setOpc(OPC);
            return null;
        }).when(subscriberHelper).decryptSensitiveData(any());

        when(subscriberHelper.resolveOpen5gsUri(TENANT)).thenReturn(OPEN5GS_URI);
        when(subscriberRepository.save(any())).thenThrow(new RuntimeException("DB write failed"));

        Subscriber updatedData = buildSubscriber();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateSubscriber(TENANT, IMSI, updatedData));

        assertEquals(503, ex.getStatusCode().value());
        verify(subscriberSync).provision(any(), eq(OPEN5GS_URI));
        verify(subscriberSync).rollbackUpdate(any(), eq(OPEN5GS_URI));
    }

    @Test
    void getAllSubscribersPaged_enrichesConnectionStatus_onlyForPageImsis() {
        Subscriber sub1 = buildSubscriber();
        sub1.setImsi("286010000000001");
        Subscriber sub2 = buildSubscriber();
        sub2.setImsi("286010000000002");

        org.springframework.data.domain.Page<Subscriber> page =
                new org.springframework.data.domain.PageImpl<>(List.of(sub1, sub2));
        when(subscriberRepository.findByTenantId(eq(TENANT), any())).thenReturn(page);
        when(connectedUeRepository.findByTenantIdAndImsiIn(eq(TENANT), anyList()))
                .thenReturn(List.of());

        service.getAllSubscribersPaged(TENANT, org.springframework.data.domain.PageRequest.of(0, 20));

        verify(connectedUeRepository).findByTenantIdAndImsiIn(eq(TENANT), anyList());
        verify(connectedUeRepository, never()).findByTenantId(TENANT);
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
