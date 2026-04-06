package com.opticoms.optinmscore.domain.edgelocation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.edgelocation.dto.EdgeLocationRequest;
import com.opticoms.optinmscore.domain.edgelocation.dto.EdgeLocationResponse;
import com.opticoms.optinmscore.domain.edgelocation.mapper.EdgeLocationMapper;
import com.opticoms.optinmscore.domain.edgelocation.model.EdgeLocation;
import com.opticoms.optinmscore.domain.edgelocation.service.EdgeLocationService;
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

@WebMvcTest(EdgeLocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class EdgeLocationControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private EdgeLocationService edgeLocationService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private EdgeLocationMapper edgeLocationMapper;

    @BeforeEach
    void setUp() {
        when(edgeLocationMapper.toResponse(any(EdgeLocation.class))).thenAnswer(inv -> {
            EdgeLocation e = inv.getArgument(0);
            EdgeLocationResponse r = new EdgeLocationResponse();
            r.setName(e.getName());
            return r;
        });
        when(edgeLocationMapper.toEntity(any(EdgeLocationRequest.class))).thenAnswer(inv -> {
            EdgeLocationRequest req = inv.getArgument(0);
            EdgeLocation el = new EdgeLocation();
            el.setName(req.getName());
            return el;
        });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201() throws Exception {
        EdgeLocation el = new EdgeLocation();
        el.setName("Istanbul-DC-1");

        when(edgeLocationService.create(eq(TENANT), any())).thenReturn(el);

        mockMvc.perform(post("/api/v1/edge-locations")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(el)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Istanbul-DC-1"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void list_returns200() throws Exception {
        EdgeLocation el = new EdgeLocation();
        el.setName("Istanbul-DC-1");

        when(edgeLocationService.list(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(el)));

        mockMvc.perform(get("/api/v1/edge-locations")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Istanbul-DC-1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/edge-locations/id-1")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(edgeLocationService).delete(TENANT, "id-1");
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void count_returns200() throws Exception {
        when(edgeLocationService.count(TENANT)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/edge-locations/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }
}
