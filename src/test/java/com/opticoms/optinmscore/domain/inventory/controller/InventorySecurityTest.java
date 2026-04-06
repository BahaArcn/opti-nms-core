package com.opticoms.optinmscore.domain.inventory.controller;

import com.opticoms.optinmscore.config.RateLimitTestConfig;
import com.opticoms.optinmscore.config.SecurityConfiguration;
import com.opticoms.optinmscore.config.security.MasterTokenFilterConfig;
import com.opticoms.optinmscore.domain.inventory.dto.NodeResourceRequest;
import com.opticoms.optinmscore.domain.inventory.dto.NodeResourceResponse;
import com.opticoms.optinmscore.domain.inventory.mapper.InventoryMapper;
import com.opticoms.optinmscore.domain.inventory.model.NodeResource;
import com.opticoms.optinmscore.domain.inventory.service.InventoryService;
import com.opticoms.optinmscore.domain.inventory.service.NodeResourceService;
import com.opticoms.optinmscore.domain.system.model.User;
import com.opticoms.optinmscore.domain.system.service.CustomUserDetailsService;
import com.opticoms.optinmscore.security.JwtAuthenticationFilter;
import com.opticoms.optinmscore.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, RateLimitTestConfig.class, MasterTokenFilterConfig.class})
class InventorySecurityTest {

    private static final String TENANT = "OPTC-0001/0001/01";
    private static final String GNB = "/api/v1/inventory/gnb";
    private static final String NODE_RESOURCES = "/api/v1/inventory/nodes/resources";
    private static final String NODE_RESOURCE_JSON = "{\"nodeId\":\"node-1\"}";

    @Autowired private MockMvc mockMvc;
    @MockBean private InventoryService inventoryService;
    @MockBean private NodeResourceService nodeResourceService;
    @MockBean private InventoryMapper inventoryMapper;
    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void noAuth_GET_403() throws Exception {
        mockMvc.perform(get(GNB)).andExpect(status().isForbidden());
    }

    @Test
    void viewerRole_GET_200() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);
        when(inventoryService.getAllGNodeBsPaged(eq(TENANT), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(GNB).header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isOk());
    }

    @Test
    void viewerRole_POST_403() throws Exception {
        User viewer = buildUser("viewer", User.Role.VIEWER);
        stubAuthentication("viewer-token", viewer);

        mockMvc.perform(post(NODE_RESOURCES)
                        .header("Authorization", "Bearer viewer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NODE_RESOURCE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorRole_POST_200() throws Exception {
        User operator = buildUser("operator", User.Role.OPERATOR);
        stubAuthentication("op-token", operator);
        NodeResource entity = new NodeResource();
        when(inventoryMapper.toNodeResourceEntity(any(NodeResourceRequest.class))).thenReturn(entity);
        when(nodeResourceService.reportNodeResource(eq(TENANT), any(NodeResource.class))).thenReturn(entity);
        when(inventoryMapper.toNodeResourceResponse(any(NodeResource.class))).thenReturn(new NodeResourceResponse());

        mockMvc.perform(post(NODE_RESOURCES)
                        .header("Authorization", "Bearer op-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NODE_RESOURCE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void adminRole_GET_200() throws Exception {
        User admin = buildUser("admin", User.Role.ADMIN);
        stubAuthentication("admin-token", admin);
        when(inventoryService.getAllGNodeBsPaged(eq(TENANT), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(GNB).header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    private void stubAuthentication(String token, User user) {
        when(jwtService.extractUsername(token)).thenReturn(user.getUsername());
        when(jwtService.extractTenantId(token)).thenReturn(TENANT);
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(true);
        when(customUserDetailsService.loadUserByUsernameAndTenantId(user.getUsername(), TENANT)).thenReturn(user);
    }

    private User buildUser(String username, User.Role role) {
        User u = new User();
        u.setId(username + "-id");
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword("encoded-password");
        u.setRole(role);
        u.setTenantId(TENANT);
        u.setActive(true);
        return u;
    }
}
