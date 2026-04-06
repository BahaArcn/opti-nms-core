package com.opticoms.optinmscore.domain.tenant.controller;

import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.UserService;
import com.opticoms.optinmscore.domain.tenant.dto.TenantOnboardRequest;
import com.opticoms.optinmscore.domain.tenant.dto.TenantRequest;
import com.opticoms.optinmscore.domain.tenant.dto.TenantResponse;
import com.opticoms.optinmscore.domain.tenant.mapper.TenantMapper;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

    @Mock private TenantService tenantService;
    @Mock private UserService userService;
    @Mock private TenantMapper tenantMapper;

    @InjectMocks
    private TenantController controller;

    @Test
    void onboardTenant_userCreationFails_tenantRolledBack() {
        Tenant saved = new Tenant();
        saved.setTenantId("VFTR-0001/0001/01");
        saved.setName("Vodafone TR");

        when(tenantService.createTenant(any())).thenReturn(saved);
        when(userService.createUser(anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Email already exists"));

        TenantOnboardRequest request = new TenantOnboardRequest();
        request.setTenantId("VFTR-0001/0001/01");
        request.setName("Vodafone TR");
        request.setAmfUrl("http://10.244.1.10:9090");
        request.setSmfUrl("http://10.244.1.11:9090");
        request.setAdminUsername("vftr-admin");
        request.setAdminEmail("admin@vftr.com");
        request.setAdminPassword("changeme123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.onboardTenant(request));

        assertEquals(409, ex.getStatusCode().value());
        verify(tenantService).hardDeleteTenant("VFTR-0001/0001/01");
    }

    @Test
    void onboardTenant_success() {
        Tenant saved = new Tenant();
        saved.setTenantId("VFTR-0001/0001/01");
        saved.setName("Vodafone TR");

        User admin = new User();
        admin.setUsername("vftr-admin");
        admin.setEmail("admin@vftr.com");

        when(tenantService.createTenant(any())).thenReturn(saved);
        when(userService.createUser(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(admin);
        when(tenantMapper.toResponse(saved)).thenReturn(tenantResponseFor(saved));

        TenantOnboardRequest request = new TenantOnboardRequest();
        request.setTenantId("VFTR-0001/0001/01");
        request.setName("Vodafone TR");
        request.setAmfUrl("http://10.244.1.10:9090");
        request.setSmfUrl("http://10.244.1.11:9090");
        request.setAdminUsername("vftr-admin");
        request.setAdminEmail("admin@vftr.com");
        request.setAdminPassword("changeme123");

        var response = controller.onboardTenant(request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("vftr-admin", response.getBody().getAdminUsername());
        verify(tenantService, never()).hardDeleteTenant(anyString());
    }

    @Test
    void getTenant_byMongoId() {
        Tenant tenant = new Tenant();
        tenant.setTenantId("VFTR-0001/0001/01");
        tenant.setName("Vodafone TR");

        when(tenantService.getTenantById("abc123")).thenReturn(tenant);
        when(tenantMapper.toResponse(tenant)).thenReturn(tenantResponseFor(tenant));

        var response = controller.getTenant("abc123");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Vodafone TR", response.getBody().getName());
        verify(tenantService).getTenantById("abc123");
    }

    @Test
    void updateTenant_byMongoId() {
        Tenant existing = new Tenant();
        existing.setTenantId("VFTR-0001/0001/01");
        existing.setName("Vodafone TR");

        Tenant updateEntity = new Tenant();
        updateEntity.setName("Vodafone TR Updated");
        updateEntity.setAmfUrl("http://10.0.0.1:7777");
        updateEntity.setSmfUrl("http://10.0.0.2:7777");

        TenantRequest updateRequest = new TenantRequest();
        updateRequest.setName("Vodafone TR Updated");
        updateRequest.setAmfUrl("http://10.0.0.1:7777");
        updateRequest.setSmfUrl("http://10.0.0.2:7777");

        Tenant saved = new Tenant();
        saved.setTenantId("VFTR-0001/0001/01");
        saved.setName("Vodafone TR Updated");

        when(tenantService.getTenantById("abc123")).thenReturn(existing);
        when(tenantMapper.toEntity(updateRequest)).thenReturn(updateEntity);
        when(tenantService.updateTenant("VFTR-0001/0001/01", updateEntity)).thenReturn(saved);
        when(tenantMapper.toResponse(saved)).thenReturn(tenantResponseFor(saved));

        var response = controller.updateTenant("abc123", updateRequest);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Vodafone TR Updated", response.getBody().getName());
        verify(tenantService).getTenantById("abc123");
        verify(tenantService).updateTenant("VFTR-0001/0001/01", updateEntity);
    }

    @Test
    void deactivateTenant_byMongoId() {
        Tenant existing = new Tenant();
        existing.setTenantId("VFTR-0001/0001/01");
        existing.setActive(true);

        Tenant deactivated = new Tenant();
        deactivated.setTenantId("VFTR-0001/0001/01");
        deactivated.setActive(false);

        when(tenantService.getTenantById("abc123")).thenReturn(existing);
        when(tenantService.deactivateTenant("VFTR-0001/0001/01")).thenReturn(deactivated);
        when(tenantMapper.toResponse(deactivated)).thenReturn(tenantResponseFor(deactivated));

        var response = controller.deactivateTenant("abc123");

        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody().isActive());
        verify(tenantService).getTenantById("abc123");
        verify(tenantService).deactivateTenant("VFTR-0001/0001/01");
    }

    private static TenantResponse tenantResponseFor(Tenant tenant) {
        TenantResponse r = new TenantResponse();
        r.setTenantId(tenant.getTenantId());
        r.setName(tenant.getName());
        r.setAmfUrl(tenant.getAmfUrl());
        r.setSmfUrl(tenant.getSmfUrl());
        r.setActive(tenant.isActive());
        return r;
    }
}
