package com.opticoms.optinmscore.integration.open5gs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Open5gsClientTest {

    private static final String AMF_URL = "http://localhost:9090";
    private static final String SMF_URL = "http://localhost:9091";

    private Open5gsClient client;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new Open5gsClient(restTemplate);
    }

    @Test
    void fetchGnbInfo_success_returnsItems() {
        Map<String, Object> gnb = Map.of("gnb_id", "000001");
        Map<String, Object> body = Map.of("items", List.of(gnb));
        when(restTemplate.getForEntity(contains("/gnb-info"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        List<Map<String, Object>> result = client.fetchGnbInfo(AMF_URL);

        assertEquals(1, result.size());
        assertEquals("000001", result.get(0).get("gnb_id"));
    }

    @Test
    void fetchGnbInfo_connectionFailed_returnsEmptyList() {
        when(restTemplate.getForEntity(contains("/gnb-info"), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        List<Map<String, Object>> result = client.fetchGnbInfo(AMF_URL);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchGnbInfo_nullBody_returnsEmptyList() {
        when(restTemplate.getForEntity(contains("/gnb-info"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(null));

        List<Map<String, Object>> result = client.fetchGnbInfo(AMF_URL);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchGnbInfo_noItemsKey_returnsEmptyList() {
        Map<String, Object> body = Map.of("pager", Map.of("count", 0));
        when(restTemplate.getForEntity(contains("/gnb-info"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        List<Map<String, Object>> result = client.fetchGnbInfo(AMF_URL);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchUeInfo_success_returnsItems() {
        Map<String, Object> ue = Map.of("supi", "imsi-286010000000001", "cm_state", "connected");
        Map<String, Object> body = Map.of("items", List.of(ue));
        when(restTemplate.getForEntity(contains("/ue-info"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        List<Map<String, Object>> result = client.fetchUeInfo(AMF_URL);

        assertEquals(1, result.size());
    }

    @Test
    void fetchUeInfo_connectionFailed_returnsEmptyList() {
        when(restTemplate.getForEntity(contains("/ue-info"), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        List<Map<String, Object>> result = client.fetchUeInfo(AMF_URL);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchPduInfo_success_returnsItems() {
        Map<String, Object> pdu = Map.of("supi", "imsi-286010000000001");
        Map<String, Object> body = Map.of("items", List.of(pdu));
        when(restTemplate.getForEntity(contains("/pdu-info?"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        List<Map<String, Object>> result = client.fetchPduInfo(SMF_URL);

        assertEquals(1, result.size());
    }

    @Test
    void fetchPduInfo_connectionFailed_returnsEmptyList() {
        when(restTemplate.getForEntity(contains("/pdu-info?"), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        List<Map<String, Object>> result = client.fetchPduInfo(SMF_URL);

        assertTrue(result.isEmpty());
    }

    @Test
    void isAmfHealthy_success_returnsTrue() {
        when(restTemplate.getForEntity(eq(AMF_URL + "/gnb-info"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));

        assertTrue(client.isAmfHealthy(AMF_URL));
    }

    @Test
    void isAmfHealthy_failure_returnsFalse() {
        when(restTemplate.getForEntity(eq(AMF_URL + "/gnb-info"), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertFalse(client.isAmfHealthy(AMF_URL));
    }

    @Test
    void isSmfHealthy_success_returnsTrue() {
        when(restTemplate.getForEntity(eq(SMF_URL + "/pdu-info"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));

        assertTrue(client.isSmfHealthy(SMF_URL));
    }

    @Test
    void isSmfHealthy_failure_returnsFalse() {
        when(restTemplate.getForEntity(eq(SMF_URL + "/pdu-info"), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertFalse(client.isSmfHealthy(SMF_URL));
    }
}
