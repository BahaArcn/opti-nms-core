package com.opticoms.optinmscore.domain.subscriber.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import com.opticoms.optinmscore.domain.subscriber.service.SubscriberService;
import com.opticoms.optinmscore.security.JwtService;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
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

@WebMvcTest(SubscriberController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriberControllerTest {

    private static final String TENANT = "OPTC-0001/0001/01";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SubscriberService subscriberService;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSubscriber_returns201() throws Exception {
        Subscriber sub = buildSubscriber();
        when(subscriberService.createSubscriber(eq(TENANT), any())).thenReturn(sub);

        mockMvc.perform(post("/api/v1/subscriber")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imsi").value("286010000000001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSubscriber_returns200() throws Exception {
        Subscriber sub = buildSubscriber();
        when(subscriberService.getSubscriber(TENANT, "286010000000001")).thenReturn(sub);

        mockMvc.perform(get("/api/v1/subscriber/286010000000001")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imsi").value("286010000000001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteSubscriber_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/subscriber/286010000000001")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isNoContent());

        verify(subscriberService).deleteSubscriber(TENANT, "286010000000001");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listSubscribers_returns200() throws Exception {
        when(subscriberService.getAllSubscribersPaged(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildSubscriber())));

        mockMvc.perform(get("/api/v1/subscriber/list")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].imsi").value("286010000000001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSubscriberCount_returns200() throws Exception {
        when(subscriberService.getSubscriberCount(TENANT)).thenReturn(42L);

        mockMvc.perform(get("/api/v1/subscriber/count")
                        .requestAttr("tenantId", TENANT))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSubscriber_returns200() throws Exception {
        Subscriber sub = buildSubscriber();
        when(subscriberService.updateSubscriber(eq(TENANT), eq("286010000000001"), any()))
                .thenReturn(sub);

        mockMvc.perform(put("/api/v1/subscriber/286010000000001")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sub)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imsi").value("286010000000001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void batchDelete_returns204() throws Exception {
        List<String> imsis = List.of("286010000000001", "286010000000002");

        mockMvc.perform(delete("/api/v1/subscriber/batch")
                        .requestAttr("tenantId", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(imsis)))
                .andExpect(status().isNoContent());

        verify(subscriberService).deleteSubscribersBatch(TENANT, imsis);
    }

    private Subscriber buildSubscriber() {
        Subscriber s = new Subscriber();
        s.setImsi("286010000000001");
        s.setKi("465B5CE8B199B49FAA5F0A2EE238A6BC");
        s.setUsimType(Subscriber.UsimType.OPC);
        s.setOpc("E8ED289DEBA952E4283B54E88E6183CA");
        s.setUeAmbrDl(1_000_000_000L);
        s.setUeAmbrUl(500_000_000L);

        Subscriber.SessionProfile profile = new Subscriber.SessionProfile();
        profile.setSst(1);
        profile.setApnDnn("internet");
        profile.setQci4g(9);
        profile.setQi5g(9);
        profile.setArpPriority(8);
        profile.setSessionAmbrDl(500_000_000L);
        profile.setSessionAmbrUl(250_000_000L);
        s.setProfileList(List.of(profile));

        return s;
    }
}
