package com.opticoms.optinmscore.domain.audit.controller;

import com.opticoms.optinmscore.domain.audit.model.AuditLog;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditAction;
import com.opticoms.optinmscore.domain.audit.model.AuditLog.AuditOutcome;
import com.opticoms.optinmscore.domain.audit.service.AuditService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private AuditLog buildLog() {
        AuditLog log = new AuditLog();
        log.setId("audit-1");
        log.setTenantId(TENANT);
        log.setUserId("user1");
        log.setUsername("admin");
        log.setAction(AuditAction.CREATE);
        log.setEntityType("Subscriber");
        log.setEntityId("sub-123");
        log.setDescription("Subscriber CREATE id=sub-123");
        log.setOutcome(AuditOutcome.SUCCESS);
        log.setTimestamp(new Date());
        return log;
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs - returns 200 with paged results")
    @WithMockUser(roles = "ADMIN")
    void listLogs_returns200() throws Exception {
        when(auditService.list(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildLog())));

        mockMvc.perform(get("/api/v1/audit/logs")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("Subscriber"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs/user/{userId} - returns 200")
    @WithMockUser(roles = "ADMIN")
    void listByUser_returns200() throws Exception {
        when(auditService.listByUserId(eq(TENANT), eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildLog())));

        mockMvc.perform(get("/api/v1/audit/logs/user/user1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("user1"));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs/entity/{entityType} - returns 200")
    @WithMockUser(roles = "ADMIN")
    void listByEntityType_returns200() throws Exception {
        when(auditService.listByEntityType(eq(TENANT), eq("Subscriber"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildLog())));

        mockMvc.perform(get("/api/v1/audit/logs/entity/Subscriber")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("Subscriber"));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs/action/{action} - returns 200")
    @WithMockUser(roles = "ADMIN")
    void listByAction_returns200() throws Exception {
        when(auditService.listByAction(eq(TENANT), eq(AuditAction.CREATE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildLog())));

        mockMvc.perform(get("/api/v1/audit/logs/action/CREATE")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("CREATE"));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs/range - returns 200")
    @WithMockUser(roles = "ADMIN")
    void listByRange_returns200() throws Exception {
        when(auditService.listByTimeRange(eq(TENANT), any(Date.class), any(Date.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildLog())));

        mockMvc.perform(get("/api/v1/audit/logs/range")
                        .requestAttr("tenantId", TENANT)
                        .param("startMs", "1000")
                        .param("endMs", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs/count - returns 200")
    @WithMockUser(roles = "ADMIN")
    void count_returns200() throws Exception {
        when(auditService.count(TENANT)).thenReturn(42L);

        mockMvc.perform(get("/api/v1/audit/logs/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(42));
    }
}
