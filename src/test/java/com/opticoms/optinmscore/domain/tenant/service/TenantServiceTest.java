package com.opticoms.optinmscore.domain.tenant.service;

import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.repository.TenantRepository;
import com.opticoms.optinmscore.security.encryption.EncryptionService;
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
class TenantServiceTest {

    private static final String TENANT_ID = "OPTC-0001/0001/01";

    @Mock private TenantRepository tenantRepository;
    @Mock private EncryptionService encryptionService;
    @InjectMocks private TenantService tenantService;

    @Test
    void createTenant_encryptsOpen5gsMongoUri() {
        Tenant tenant = buildTenant();
        when(tenantRepository.existsByTenantId(TENANT_ID)).thenReturn(false);
        when(encryptionService.encrypt("mongodb://localhost:27017/open5gs")).thenReturn("encrypted-uri");
        when(encryptionService.decrypt("encrypted-uri")).thenReturn("mongodb://localhost:27017/open5gs");
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tenant result = tenantService.createTenant(tenant);

        verify(encryptionService).encrypt("mongodb://localhost:27017/open5gs");
        assertEquals("mongodb://localhost:27017/open5gs", result.getOpen5gsMongoUri());
    }

    @Test
    void getTenant_decryptsOpen5gsMongoUri() {
        Tenant stored = buildTenant();
        stored.setOpen5gsMongoUri("encrypted-uri");
        when(tenantRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(stored));
        when(encryptionService.decrypt("encrypted-uri")).thenReturn("mongodb://localhost:27017/open5gs");

        Tenant result = tenantService.getTenant(TENANT_ID);

        assertEquals("mongodb://localhost:27017/open5gs", result.getOpen5gsMongoUri());
    }

    @Test
    void getTenant_backwardCompat_plainTextUri_notDecrypted() {
        Tenant stored = buildTenant();
        stored.setOpen5gsMongoUri("mongodb://localhost:27017/open5gs");
        when(tenantRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(stored));
        when(encryptionService.decrypt("mongodb://localhost:27017/open5gs"))
                .thenThrow(new RuntimeException("not encrypted"));

        Tenant result = tenantService.getTenant(TENANT_ID);

        assertNotNull(result);
    }

    @Test
    void createTenant_duplicateTenantId_throws409() {
        Tenant tenant = buildTenant();
        when(tenantRepository.existsByTenantId(TENANT_ID)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tenantService.createTenant(tenant));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void deactivateTenant_setsActiveFalse() {
        Tenant existing = buildTenant();
        when(tenantRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(encryptionService.decrypt(any())).thenAnswer(inv -> inv.getArgument(0));
        when(encryptionService.encrypt(any())).thenAnswer(inv -> inv.getArgument(0));

        Tenant result = tenantService.deactivateTenant(TENANT_ID);

        assertFalse(result.isActive());
    }

    private Tenant buildTenant() {
        Tenant t = new Tenant();
        t.setTenantId(TENANT_ID);
        t.setName("Test Tenant");
        t.setAmfUrl("http://amf:9090");
        t.setSmfUrl("http://smf:9091");
        t.setOpen5gsMongoUri("mongodb://localhost:27017/open5gs");
        t.setActive(true);
        return t;
    }
}
