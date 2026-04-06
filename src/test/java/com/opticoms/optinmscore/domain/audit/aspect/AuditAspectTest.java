package com.opticoms.optinmscore.domain.audit.aspect;

import com.opticoms.optinmscore.domain.audit.model.AuditLog;
import com.opticoms.optinmscore.domain.audit.service.AuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock private AuditService auditService;
    @Mock private ProceedingJoinPoint pjp;
    @InjectMocks private AuditAspect auditAspect;

    @Test
    void audit_successfulMethod_logsSuccessOutcome() throws Throwable {
        Audited audited = mockAudited(AuditLog.AuditAction.CREATE, "Subscriber");
        when(pjp.proceed()).thenReturn(null);
        when(pjp.getArgs()).thenReturn(new Object[]{"OPTC-0001/0001/01"});

        auditAspect.audit(pjp, audited);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).log(captor.capture());
        assertEquals(AuditLog.AuditOutcome.SUCCESS, captor.getValue().getOutcome());
    }

    @Test
    void audit_failingMethod_logsFailureOutcome() throws Throwable {
        Audited audited = mockAudited(AuditLog.AuditAction.DELETE, "User");
        when(pjp.proceed()).thenThrow(new RuntimeException("DB error"));
        when(pjp.getArgs()).thenReturn(new Object[]{"OPTC-0001/0001/01", "user-123"});

        assertThrows(RuntimeException.class, () -> auditAspect.audit(pjp, audited));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).log(captor.capture());
        assertEquals(AuditLog.AuditOutcome.FAILURE, captor.getValue().getOutcome());
        assertEquals("DB error", captor.getValue().getFailureReason());
    }

    @Test
    void extractTenantId_fromRequestAttribute_preferredOverArgs() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("tenantId", "REQT-0001/0001/01");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Audited audited = mockAudited(AuditLog.AuditAction.CREATE, "Policy");
        when(pjp.proceed()).thenReturn(null);
        lenient().when(pjp.getArgs()).thenReturn(new Object[]{"ARGT-0002/0002/02"});

        auditAspect.audit(pjp, audited);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).log(captor.capture());
        assertEquals("REQT-0001/0001/01", captor.getValue().getTenantId());

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void extractTenantId_fallsBackToTenantIdFormatArg() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        Audited audited = mockAudited(AuditLog.AuditAction.UPDATE, "Subscriber");
        when(pjp.proceed()).thenReturn(null);
        when(pjp.getArgs()).thenReturn(new Object[]{"OPTC-0001/0001/01", "some-data"});

        auditAspect.audit(pjp, audited);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).log(captor.capture());
        assertEquals("OPTC-0001/0001/01", captor.getValue().getTenantId());
    }

    @Test
    void extractTenantId_randomStringArg_returnsSystem() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        Audited audited = mockAudited(AuditLog.AuditAction.CREATE, "Backup");
        when(pjp.proceed()).thenReturn(null);
        when(pjp.getArgs()).thenReturn(new Object[]{"SCHEDULER"});

        auditAspect.audit(pjp, audited);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).log(captor.capture());
        assertEquals("SYSTEM", captor.getValue().getTenantId());
    }

    private Audited mockAudited(AuditLog.AuditAction action, String entityType) {
        Audited audited = mock(Audited.class);
        when(audited.action()).thenReturn(action);
        when(audited.entityType()).thenReturn(entityType);
        return audited;
    }
}
