package com.opticoms.optinmscore.domain.audit.service;

import com.opticoms.optinmscore.domain.audit.model.AuditLog;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditOutcome;
import com.opticoms.optinmscore.domain.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditService auditService;

    private AuditLog buildEntry() {
        AuditLog entry = new AuditLog();
        entry.setTenantId("OPTC-0001/0001/01");
        entry.setUserId("user1");
        entry.setUsername("admin");
        entry.setAction(AuditAction.CREATE);
        entry.setEntityType("Subscriber");
        entry.setEntityId("sub-123");
        entry.setDescription("Subscriber CREATE id=sub-123");
        entry.setOutcome(AuditOutcome.SUCCESS);
        return entry;
    }

    @Test
    @DisplayName("log - saves to repository")
    void log_savesToRepository() {
        AuditLog entry = buildEntry();

        auditService.log(entry);

        verify(repository).save(entry);
    }

    @Test
    @DisplayName("log - when repository throws, does not propagate exception")
    void log_whenRepositoryThrows_doesNotPropagateException() {
        AuditLog entry = buildEntry();
        doThrow(new RuntimeException("DB error")).when(repository).save(any());

        assertThatCode(() -> auditService.log(entry)).doesNotThrowAnyException();
        verify(repository).save(entry);
    }

    @Test
    @DisplayName("list - delegates to repository")
    void list_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<AuditLog> expected = new PageImpl<>(List.of(buildEntry()));
        when(repository.findByTenantId("OPTC-0001/0001/01", pageable)).thenReturn(expected);

        Page<AuditLog> result = auditService.list("OPTC-0001/0001/01", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repository).findByTenantId("OPTC-0001/0001/01", pageable);
    }

    @Test
    @DisplayName("listByUserId - delegates to repository")
    void listByUserId_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<AuditLog> expected = new PageImpl<>(List.of(buildEntry()));
        when(repository.findByTenantIdAndUserId("OPTC-0001/0001/01", "user1", pageable)).thenReturn(expected);

        Page<AuditLog> result = auditService.listByUserId("OPTC-0001/0001/01", "user1", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repository).findByTenantIdAndUserId("OPTC-0001/0001/01", "user1", pageable);
    }

    @Test
    @DisplayName("listByEntityType - delegates to repository")
    void listByEntityType_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<AuditLog> expected = new PageImpl<>(List.of(buildEntry()));
        when(repository.findByTenantIdAndEntityType("OPTC-0001/0001/01", "Subscriber", pageable)).thenReturn(expected);

        Page<AuditLog> result = auditService.listByEntityType("OPTC-0001/0001/01", "Subscriber", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByAction - delegates to repository")
    void listByAction_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<AuditLog> expected = new PageImpl<>(List.of(buildEntry()));
        when(repository.findByTenantIdAndAction("OPTC-0001/0001/01", AuditAction.CREATE, pageable)).thenReturn(expected);

        Page<AuditLog> result = auditService.listByAction("OPTC-0001/0001/01", AuditAction.CREATE, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("listByTimeRange - delegates to repository")
    void listByTimeRange_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 50);
        Date start = new Date(1000L);
        Date end = new Date(2000L);
        Page<AuditLog> expected = new PageImpl<>(List.of(buildEntry()));
        when(repository.findByTenantIdAndTimestampBetween("OPTC-0001/0001/01", start, end, pageable)).thenReturn(expected);

        Page<AuditLog> result = auditService.listByTimeRange("OPTC-0001/0001/01", start, end, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("count - delegates to repository")
    void count_delegatesToRepository() {
        when(repository.countByTenantId("OPTC-0001/0001/01")).thenReturn(42L);

        long result = auditService.count("OPTC-0001/0001/01");

        assertThat(result).isEqualTo(42);
        verify(repository).countByTenantId("OPTC-0001/0001/01");
    }
}
