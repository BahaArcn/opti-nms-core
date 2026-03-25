package com.opticoms.optinmscore.domain.tenant.controller;

import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.UserService;
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

        TenantController.TenantOnboardRequest request = new TenantController.TenantOnboardRequest();
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

        TenantController.TenantOnboardRequest request = new TenantController.TenantOnboardRequest();
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
}
