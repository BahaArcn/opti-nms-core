package com.opticoms.optinmscore.domain.observability.service;

import com.opticoms.optinmscore.domain.observability.model.Alarm;
import com.opticoms.optinmscore.domain.observability.repository.AlarmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Mock private AlarmRepository alarmRepository;

    @InjectMocks
    private AlarmService service;

    private Alarm alarm;

    @BeforeEach
    void setUp() {
        alarm = buildAlarm("gNodeB-001", "LINK_DOWN", Alarm.Severity.CRITICAL);
        SecurityContextHolder.clearContext();
    }

    @Test
    void raiseAlarm_newAlarm_createdTrue() {
        when(alarmRepository.findByTenantIdAndSourceAndAlarmTypeAndStatus(
                TENANT, "gNodeB-001", "LINK_DOWN", Alarm.AlarmStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(alarmRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AlarmService.RaiseResult result = service.raiseAlarm(TENANT, alarm);

        assertTrue(result.created());
        assertEquals(Alarm.AlarmStatus.ACTIVE, result.alarm().getStatus());
        assertEquals(TENANT, result.alarm().getTenantId());
        verify(alarmRepository).save(any());
    }

    @Test
    void raiseAlarm_duplicate_updatesExisting() {
        Alarm existing = buildAlarm("gNodeB-001", "LINK_DOWN", Alarm.Severity.CRITICAL);
        existing.setStatus(Alarm.AlarmStatus.ACTIVE);

        when(alarmRepository.findByTenantIdAndSourceAndAlarmTypeAndStatus(
                TENANT, "gNodeB-001", "LINK_DOWN", Alarm.AlarmStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(alarmRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        alarm.setDescription("Updated description");
        AlarmService.RaiseResult result = service.raiseAlarm(TENANT, alarm);

        assertFalse(result.created());
        assertEquals("Updated description", result.alarm().getDescription());
    }

    @Test
    void clearAlarm_activeExists_setsCleared() {
        Alarm active = buildAlarm("gNodeB-001", "LINK_DOWN", Alarm.Severity.CRITICAL);
        active.setStatus(Alarm.AlarmStatus.ACTIVE);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("operator1", null, Collections.emptyList()));

        when(alarmRepository.findByTenantIdAndSourceAndAlarmTypeAndStatus(
                TENANT, "gNodeB-001", "LINK_DOWN", Alarm.AlarmStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        when(alarmRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.clearAlarm(TENANT, "gNodeB-001", "LINK_DOWN");

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmRepository).save(captor.capture());
        Alarm saved = captor.getValue();
        assertEquals(Alarm.AlarmStatus.CLEARED, saved.getStatus());
        assertEquals("operator1", saved.getClearedBy());
        assertNotNull(saved.getClearedTime());
    }

    @Test
    void clearAlarm_noAuthentication_usesSystemUser() {
        Alarm active = buildAlarm("gNodeB-001", "LINK_DOWN", Alarm.Severity.CRITICAL);
        active.setStatus(Alarm.AlarmStatus.ACTIVE);

        when(alarmRepository.findByTenantIdAndSourceAndAlarmTypeAndStatus(
                TENANT, "gNodeB-001", "LINK_DOWN", Alarm.AlarmStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        when(alarmRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.clearAlarm(TENANT, "gNodeB-001", "LINK_DOWN");

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmRepository).save(captor.capture());
        assertEquals("system", captor.getValue().getClearedBy());
    }

    @Test
    void clearAlarm_noActiveAlarm_doesNothing() {
        when(alarmRepository.findByTenantIdAndSourceAndAlarmTypeAndStatus(
                TENANT, "gNodeB-001", "LINK_DOWN", Alarm.AlarmStatus.ACTIVE))
                .thenReturn(Optional.empty());

        service.clearAlarm(TENANT, "gNodeB-001", "LINK_DOWN");

        verify(alarmRepository, never()).save(any());
    }

    @Test
    void getAlarms_noFilters_returnsAll() {
        List<Alarm> expected = List.of(alarm);
        when(alarmRepository.findByTenantIdOrderByEventTimeDesc(TENANT)).thenReturn(expected);

        List<Alarm> result = service.getAlarms(TENANT, null, null);

        assertEquals(expected, result);
    }

    @Test
    void getAlarms_severityFilter() {
        when(alarmRepository.findByTenantIdAndSeverityOrderByEventTimeDesc(TENANT, Alarm.Severity.CRITICAL))
                .thenReturn(List.of(alarm));

        List<Alarm> result = service.getAlarms(TENANT, Alarm.Severity.CRITICAL, null);

        assertEquals(1, result.size());
    }

    @Test
    void getAlarms_statusFilter() {
        when(alarmRepository.findByTenantIdAndStatusOrderByEventTimeDesc(TENANT, Alarm.AlarmStatus.ACTIVE))
                .thenReturn(List.of(alarm));

        List<Alarm> result = service.getAlarms(TENANT, null, Alarm.AlarmStatus.ACTIVE);

        assertEquals(1, result.size());
    }

    @Test
    void getAlarms_bothFilters() {
        when(alarmRepository.findByTenantIdAndSeverityAndStatusOrderByEventTimeDesc(
                TENANT, Alarm.Severity.CRITICAL, Alarm.AlarmStatus.ACTIVE))
                .thenReturn(List.of(alarm));

        List<Alarm> result = service.getAlarms(TENANT, Alarm.Severity.CRITICAL, Alarm.AlarmStatus.ACTIVE);

        assertEquals(1, result.size());
    }

    @Test
    void getAlarmById_found() {
        when(alarmRepository.findByIdAndTenantId("alarm-1", TENANT)).thenReturn(Optional.of(alarm));

        Alarm result = service.getAlarmById(TENANT, "alarm-1");

        assertEquals(alarm, result);
    }

    @Test
    void getAlarmById_notFound_throws404() {
        when(alarmRepository.findByIdAndTenantId("missing", TENANT)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getAlarmById(TENANT, "missing"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void countActiveAlarms_delegatesToRepo() {
        when(alarmRepository.countByTenantIdAndStatus(TENANT, Alarm.AlarmStatus.ACTIVE)).thenReturn(5L);

        assertEquals(5L, service.countActiveAlarms(TENANT));
    }

    @Test
    void countBySeverity_delegatesToRepo() {
        when(alarmRepository.countByTenantIdAndSeverityAndStatus(
                TENANT, Alarm.Severity.MAJOR, Alarm.AlarmStatus.ACTIVE)).thenReturn(3L);

        assertEquals(3L, service.countBySeverity(TENANT, Alarm.Severity.MAJOR));
    }

    @Test
    void raiseAlarm_duplicateKeyOnSave_returnsExistingAlarm() {
        Alarm existing = buildAlarm("gNodeB-001", "LINK_DOWN", Alarm.Severity.CRITICAL);
        existing.setStatus(Alarm.AlarmStatus.ACTIVE);

        when(alarmRepository.findByTenantIdAndSourceAndAlarmTypeAndStatus(
                TENANT, "gNodeB-001", "LINK_DOWN", Alarm.AlarmStatus.ACTIVE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(alarmRepository.save(any()))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate key"));

        AlarmService.RaiseResult result = service.raiseAlarm(TENANT, alarm);

        assertFalse(result.created());
        assertEquals(existing, result.alarm());
        verify(alarmRepository, times(2))
                .findByTenantIdAndSourceAndAlarmTypeAndStatus(
                        TENANT, "gNodeB-001", "LINK_DOWN", Alarm.AlarmStatus.ACTIVE);
    }

    private Alarm buildAlarm(String source, String alarmType, Alarm.Severity severity) {
        Alarm a = new Alarm();
        a.setSource(source);
        a.setAlarmType(alarmType);
        a.setSeverity(severity);
        a.setDescription("Test alarm");
        return a;
    }
}
