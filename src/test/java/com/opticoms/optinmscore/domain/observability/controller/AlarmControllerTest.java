package com.opticoms.optinmscore.domain.observability.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.observability.dto.AlarmRequest;
import com.opticoms.optinmscore.domain.observability.dto.AlarmResponse;
import com.opticoms.optinmscore.domain.observability.mapper.AlarmMapper;
import com.opticoms.optinmscore.domain.observability.model.Alarm;
import com.opticoms.optinmscore.domain.observability.service.AlarmService;
import com.opticoms.optinmscore.security.JwtService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlarmController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlarmControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AlarmService alarmService;
    @MockBean private AlarmMapper alarmMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void stubAlarmMapper() {
        when(alarmMapper.toResponse(any(Alarm.class))).thenAnswer(invocation -> {
            Alarm a = invocation.getArgument(0);
            AlarmResponse r = new AlarmResponse();
            r.setSource(a.getSource());
            r.setAlarmType(a.getAlarmType());
            r.setDescription(a.getDescription());
            r.setSeverity(a.getSeverity());
            r.setStatus(a.getStatus());
            return r;
        });
        when(alarmMapper.toEntity(any(AlarmRequest.class))).thenAnswer(invocation -> {
            AlarmRequest req = invocation.getArgument(0);
            Alarm a = new Alarm();
            a.setSource(req.getSource());
            a.setAlarmType(req.getAlarmType());
            a.setDescription(req.getDescription());
            a.setSeverity(req.getSeverity());
            return a;
        });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAlarms_returns200() throws Exception {
        Alarm alarm = new Alarm();
        alarm.setSource("gNodeB-001");
        alarm.setAlarmType("LINK_DOWN");
        alarm.setSeverity(Alarm.Severity.CRITICAL);

        when(alarmService.getAlarms(eq(TENANT), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alarm)));

        mockMvc.perform(get("/api/v1/fault/alarms")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].source").value("gNodeB-001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAlarms_withSeverityFilter() throws Exception {
        when(alarmService.getAlarms(eq(TENANT), eq(Alarm.Severity.CRITICAL), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/fault/alarms")
                        .requestAttr("tenantId", TENANT)
                        .param("severity", "CRITICAL"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void raiseAlarm_newAlarm_returns201() throws Exception {
        Alarm alarm = new Alarm();
        alarm.setSource("gNodeB-001");
        alarm.setAlarmType("LINK_DOWN");
        alarm.setSeverity(Alarm.Severity.CRITICAL);
        alarm.setDescription("Test alarm");

        AlarmRequest request = new AlarmRequest();
        request.setSource("gNodeB-001");
        request.setAlarmType("LINK_DOWN");
        request.setSeverity(Alarm.Severity.CRITICAL);
        request.setDescription("Test alarm");

        when(alarmService.raiseAlarm(eq(TENANT), any()))
                .thenReturn(new AlarmService.RaiseResult(alarm, true));

        mockMvc.perform(post("/api/v1/fault/alarms")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("gNodeB-001"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void raiseAlarm_duplicate_returns200() throws Exception {
        Alarm alarm = new Alarm();
        alarm.setSource("gNodeB-001");
        alarm.setAlarmType("LINK_DOWN");
        alarm.setSeverity(Alarm.Severity.CRITICAL);

        AlarmRequest request = new AlarmRequest();
        request.setSource("gNodeB-001");
        request.setAlarmType("LINK_DOWN");
        request.setSeverity(Alarm.Severity.CRITICAL);

        when(alarmService.raiseAlarm(eq(TENANT), any()))
                .thenReturn(new AlarmService.RaiseResult(alarm, false));

        mockMvc.perform(post("/api/v1/fault/alarms")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void clearAlarm_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/fault/alarms/clear")
                        .requestAttr("tenantId", TENANT)
                        .param("source", "gNodeB-001")
                        .param("alarmType", "LINK_DOWN"))
                .andExpect(status().isOk());

        verify(alarmService).clearAlarm(TENANT, "gNodeB-001", "LINK_DOWN");
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void acknowledgeAlarm_returns200() throws Exception {
        Alarm alarm = new Alarm();
        alarm.setSource("gNodeB-001");
        alarm.setStatus(Alarm.AlarmStatus.ACKNOWLEDGED);

        when(alarmService.acknowledgeAlarm(eq(TENANT), eq("alarm-1"))).thenReturn(alarm);

        mockMvc.perform(put("/api/v1/fault/alarms/alarm-1/acknowledge")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAlarmById_returns200() throws Exception {
        Alarm alarm = new Alarm();
        alarm.setSource("gNodeB-001");
        when(alarmService.getAlarmById(TENANT, "alarm-1")).thenReturn(alarm);

        mockMvc.perform(get("/api/v1/fault/alarms/alarm-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("gNodeB-001"));
    }
}
