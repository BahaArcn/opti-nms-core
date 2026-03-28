package com.opticoms.optinmscore.domain.license.service;

import com.opticoms.optinmscore.domain.apn.repository.ApnProfileRepository;
import com.opticoms.optinmscore.domain.edgelocation.repository.EdgeLocationRepository;
import com.opticoms.optinmscore.domain.inventory.repository.GNodeBRepository;
import com.opticoms.optinmscore.domain.license.model.License;
import com.opticoms.optinmscore.domain.license.repository.LicenseRepository;
import com.opticoms.optinmscore.domain.subscriber.repository.SubscriberRepository;
import com.opticoms.optinmscore.domain.system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LicenseServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private LicenseRepository licenseRepository;
    @Mock private SubscriberRepository subscriberRepository;
    @Mock private GNodeBRepository gNodeBRepository;
    @Mock private ApnProfileRepository apnProfileRepository;
    @Mock private EdgeLocationRepository edgeLocationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private LicenseService service;

    private License license;

    @BeforeEach
    void setUp() {
        license = new License();
        license.setMaxSubscribers(100);
        license.setMaxGNodeBs(10);
        license.setMaxDnns(5);
        license.setActive(true);
    }

    @Test
    void createOrUpdateLicense_newLicense_creates() {
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(licenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        License result = service.createOrUpdateLicense(TENANT, license);

        assertEquals(TENANT, result.getTenantId());
        verify(licenseRepository).save(any());
    }

    @Test
    void createOrUpdateLicense_existingLicense_updates() {
        License existing = new License();
        existing.setId("lic-1");
        existing.setTenantId(TENANT);
        existing.setMaxSubscribers(50);

        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.of(existing));
        when(licenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        License result = service.createOrUpdateLicense(TENANT, license);

        assertEquals("lic-1", result.getId());
        assertEquals(100, result.getMaxSubscribers());
    }

    @Test
    void checkCanAddSubscriber_noLicense_passes() {
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.checkCanAddSubscriber(TENANT));
    }

    @Test
    void checkCanAddSubscriber_withinLimit_passes() {
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.of(license));
        when(subscriberRepository.countByTenantId(TENANT)).thenReturn(50L);

        assertDoesNotThrow(() -> service.checkCanAddSubscriber(TENANT));
    }

    @Test
    void checkCanAddSubscriber_atLimit_throwsForbidden() {
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.of(license));
        when(subscriberRepository.countByTenantId(TENANT)).thenReturn(100L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.checkCanAddSubscriber(TENANT));
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("No sufficient licenses"));
    }

    @Test
    void checkCanAddSubscriber_inactiveLicense_throwsForbidden() {
        license.setActive(false);
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.of(license));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.checkCanAddSubscriber(TENANT));
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("not active"));
    }

    @Test
    void checkCanAddSubscriber_expiredLicense_gracePeriodPasses() {
        license.setExpiresAt(System.currentTimeMillis() - 1000);
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.of(license));

        assertDoesNotThrow(() -> service.checkCanAddSubscriber(TENANT));
    }

    @Test
    void checkCanAddSubscriber_nullLimit_passes() {
        license.setMaxSubscribers(null);
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.of(license));
        when(subscriberRepository.countByTenantId(TENANT)).thenReturn(9999L);

        assertDoesNotThrow(() -> service.checkCanAddSubscriber(TENANT));
    }

    @Test
    void getLicenseStatus_noLicense_returnsNotPresent() {
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());

        LicenseService.LicenseStatus status = service.getLicenseStatus(TENANT);

        assertFalse(status.isLicensePresent());
        assertFalse(status.isActive());
    }

    @Test
    void getLicenseStatus_activeLicense_returnsCorrectCounts() {
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.of(license));
        when(subscriberRepository.countByTenantId(TENANT)).thenReturn(42L);
        when(gNodeBRepository.countByTenantId(TENANT)).thenReturn(3L);
        when(apnProfileRepository.countByTenantId(TENANT)).thenReturn(2L);
        when(edgeLocationRepository.countByTenantId(TENANT)).thenReturn(1L);

        LicenseService.LicenseStatus status = service.getLicenseStatus(TENANT);

        assertTrue(status.isLicensePresent());
        assertTrue(status.isActive());
        assertEquals(42, status.getCurrentSubscribers());
        assertEquals(3, status.getCurrentGNodeBs());
        assertEquals(100, status.getMaxSubscribers());
    }

    @Test
    void getLicense_notFound_throws404() {
        when(licenseRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getLicense(TENANT));
        assertEquals(404, ex.getStatusCode().value());
    }
}
