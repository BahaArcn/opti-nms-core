package com.opticoms.optinmscore.integration.open5gs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client for Open5GS InfoAPI endpoints.
 * See: https://open5gs.org/open5gs/docs/tutorial/07-infoAPI-UE-gNB-session-data/
 *
 * AMF: /gnb-info, /ue-info (port 9090)
 * SMF: /pdu-info          (port 9090)
 *
 * Response format: { "items": [...], "pager": { "page", "page_size", "count" } }
 *
 * All methods accept explicit URL parameters — no hardcoded base URLs.
 * Tenant-specific URLs are stored in the Tenant document and passed by callers.
 */
@Slf4j
@Component
public class Open5gsClient {

    private final RestTemplate restTemplate;

    public Open5gsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchGnbInfo(String amfUrl) {
        try {
            String url = amfUrl + "/gnb-info?page=-1&page_size=100";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return extractItems(response);
        } catch (RestClientException e) {
            log.warn("Failed to fetch gNB info from AMF ({}): {}", amfUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchUeInfo(String amfUrl) {
        try {
            String url = amfUrl + "/ue-info?page=-1&page_size=100";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return extractItems(response);
        } catch (RestClientException e) {
            log.warn("Failed to fetch UE info from AMF ({}): {}", amfUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchPduInfo(String smfUrl) {
        try {
            String url = smfUrl + "/pdu-info?page=-1&page_size=100";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return extractItems(response);
        } catch (RestClientException e) {
            log.warn("Failed to fetch PDU session info from SMF ({}): {}", smfUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean isAmfHealthy(String amfUrl) {
        try {
            restTemplate.getForEntity(amfUrl + "/gnb-info", Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSmfHealthy(String smfUrl) {
        try {
            restTemplate.getForEntity(smfUrl + "/pdu-info", Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isUpfHealthy(String upfMetricsUrl) {
        if (upfMetricsUrl == null || upfMetricsUrl.isBlank()) {
            return false;
        }
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(upfMetricsUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(ResponseEntity<Map> response) {
        if (response.getBody() == null) {
            return Collections.emptyList();
        }
        Map<String, Object> body = response.getBody();
        if (body.containsKey("items")) {
            return (List<Map<String, Object>>) body.get("items");
        }
        return Collections.emptyList();
    }
}
