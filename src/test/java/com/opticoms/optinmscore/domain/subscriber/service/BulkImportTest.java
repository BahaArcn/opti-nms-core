package com.opticoms.optinmscore.domain.subscriber.service;

import com.opticoms.optinmscore.domain.license.service.LicenseService;
import com.opticoms.optinmscore.domain.subscriber.model.BulkImportResult;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.subscriber.repository.SubscriberRepository;
import com.opticoms.optinmscore.integration.open5gs.Open5gsProvisioningService;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkImportTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String KI = "465B5CE8B199B49FAA5F0A2EE238A6BC";
    private static final String OPC = "E8ED289DEBA952E4283B54E88E6183CA";

    @Mock private SubscriberRepository subscriberRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private Open5gsProvisioningService open5gsProvisioning;
    @Mock private LicenseService licenseService;
    @Mock private SubscriberHelper subscriberHelper;

    @InjectMocks
    private BulkImportService service;

    @BeforeEach
    void setUp() {
        lenient().when(encryptionService.hash(anyString())).thenAnswer(inv -> "hash-" + inv.getArgument(0));
        lenient().when(encryptionService.encrypt(anyString())).thenReturn("encrypted");
        lenient().when(open5gsProvisioning.provisionSubscribersBulk(anyList(), any()))
                .thenAnswer(inv -> {
                    List<?> subs = inv.getArgument(0);
                    return new Open5gsProvisioningService.BulkProvisionResult(
                            subs.size(), 0, 0, List.of());
                });
        lenient().when(subscriberRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(subscriberRepository.findByTenantIdAndImsiHashIn(eq(TENANT), anyCollection()))
                .thenReturn(List.of());

        lenient().doNothing().when(subscriberHelper).validateKeys(any());
        lenient().doNothing().when(subscriberHelper).enrichProfilesFromApn(any(), any());
        lenient().when(subscriberHelper.resolveOpen5gsUri(any())).thenReturn("mongodb://localhost:27018/open5gs");
        lenient().doNothing().when(subscriberHelper).encryptSensitiveData(any());
    }

    @Test
    void bulkImport_allValid_noLicense_importsAll() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(10);
        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(10, result.getTotalInFile());
        assertEquals(10, result.getSuccessCount());
        assertEquals(0, result.getFailedValidation());
        assertEquals(0, result.getSkippedDueToLicense());
        assertTrue(result.getMessage().contains("All 10"));
    }

    @Test
    void bulkImport_licenseTrim_importsPartial() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(5);

        List<Subscriber> parsed = buildSubscribers(10);
        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(10, result.getTotalInFile());
        assertEquals(5, result.getSuccessCount());
        assertEquals(5, result.getSkippedDueToLicense());
        assertTrue(result.getMessage().contains("License limit"));
    }

    @Test
    void bulkImport_inactiveLicense_throws403() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(0);

        List<Subscriber> parsed = buildSubscribers(5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.bulkImport(TENANT, parsed));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void bulkImport_duplicateImsiInFile_skipsSecond() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(3);
        parsed.get(2).setImsi(parsed.get(0).getImsi());

        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(3, result.getTotalInFile());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getSkippedDuplicateInFile());
    }

    @Test
    void bulkImport_duplicateImsiInDb_skips() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(3);

        Subscriber existingInDb = new Subscriber();
        existingInDb.setImsiHash("hash-" + parsed.get(0).getImsi());
        when(subscriberRepository.findByTenantIdAndImsiHashIn(eq(TENANT), anyCollection()))
                .thenReturn(List.of(existingInDb));

        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(3, result.getTotalInFile());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getSkippedDuplicateInDb());
    }

    @Test
    void bulkImport_invalidImsi_failsValidation() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(3);
        parsed.get(1).setImsi("12345");

        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(3, result.getTotalInFile());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailedValidation());
        assertFalse(result.getErrors().isEmpty());
        assertEquals("imsi", result.getErrors().get(0).getField());
    }

    @Test
    void bulkImport_invalidKi_failsValidation() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(2);
        parsed.get(0).setKi("SHORT");

        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "Ki must be exactly 16 bytes (32 hex chars)"))
                .when(subscriberHelper).validateKeys(argThat(s -> "SHORT".equals(s.getKi())));

        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(2, result.getTotalInFile());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedValidation());
    }

    @Test
    void bulkImport_missingUsimType_failsValidation() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(2);
        parsed.get(0).setUsimType(null);

        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedValidation());
        assertEquals("usimType", result.getErrors().get(0).getField());
    }

    @Test
    void bulkImport_emptyProfileList_failsValidation() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(2);
        parsed.get(0).setProfileList(List.of());

        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedValidation());
        assertEquals("profileList", result.getErrors().get(0).getField());
    }

    @Test
    void bulkImport_allInvalid_zeroSuccess() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(3);
        parsed.forEach(s -> s.setImsi("bad"));

        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(3, result.getTotalInFile());
        assertEquals(0, result.getSuccessCount());
        assertEquals(3, result.getFailedValidation());
    }

    @Test
    void bulkImport_expiredLicense_gracePeriod_importsAll() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(5);
        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(5, result.getSuccessCount());
        assertEquals(0, result.getSkippedDueToLicense());
    }

    @Test
    void bulkImport_nullLimit_unlimited() {
        when(licenseService.getRemainingSubscriberQuota(TENANT)).thenReturn(Integer.MAX_VALUE);

        List<Subscriber> parsed = buildSubscribers(100);
        BulkImportResult result = service.bulkImport(TENANT, parsed);

        assertEquals(100, result.getSuccessCount());
        assertEquals(0, result.getSkippedDueToLicense());
    }

    private List<Subscriber> buildSubscribers(int count) {
        List<Subscriber> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Subscriber s = new Subscriber();
            s.setImsi(String.format("28601%010d", i + 1));
            s.setKi(KI);
            s.setUsimType(Subscriber.UsimType.OPC);
            s.setOpc(OPC);
            s.setUeAmbrDl(1_000_000_000L);
            s.setUeAmbrUl(500_000_000L);

            Subscriber.SessionProfile profile = new Subscriber.SessionProfile();
            profile.setSst(1);
            profile.setApnDnn("internet");
            s.setProfileList(List.of(profile));

            list.add(s);
        }
        return list;
    }
}
