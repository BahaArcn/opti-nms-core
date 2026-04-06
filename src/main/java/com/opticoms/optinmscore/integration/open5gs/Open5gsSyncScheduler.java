package com.opticoms.optinmscore.integration.open5gs;

import com.opticoms.optinmscore.domain.inventory.model.ConnectedUe;
import com.opticoms.optinmscore.domain.inventory.model.GNodeB;
import com.opticoms.optinmscore.domain.inventory.model.PduSession;
import com.opticoms.optinmscore.domain.inventory.repository.ConnectedUeRepository;
import com.opticoms.optinmscore.domain.inventory.repository.GNodeBRepository;
import com.opticoms.optinmscore.domain.inventory.repository.PduSessionRepository;
import com.opticoms.optinmscore.domain.observability.model.Alarm;
import com.opticoms.optinmscore.domain.observability.service.AlarmService;
import com.opticoms.optinmscore.domain.performance.model.PmMetric;
import com.opticoms.optinmscore.domain.performance.service.PmService;
import com.opticoms.optinmscore.domain.tenant.model.Tenant;
import com.opticoms.optinmscore.domain.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Scheduled task that syncs data from Open5GS InfoAPI every 30 seconds.
 *
 * Iterates over all active tenants and syncs each tenant's Open5GS data
 * using the per-tenant AMF/SMF URLs stored in the Tenant document.
 *
 * Response formats (from real Open5GS InfoAPI):
 *   /gnb-info -> items[]: { gnb_id, plmn, ng.sctp.peer, supported_ta_list, num_connected_ues }
 *   /ue-info  -> items[]: { supi, cm_state, gnb.gnb_id, location.nr_tai, security, pdu_sessions }
 *   /pdu-info -> items[]: { supi, pdu[]: { psi, dnn, ipv4, snssai, pdu_state }, ue_activity }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Open5gsSyncScheduler {

    private final Open5gsClient open5gsClient;
    private final GNodeBRepository gNodeBRepository;
    private final ConnectedUeRepository connectedUeRepository;
    private final PduSessionRepository pduSessionRepository;
    private final AlarmService alarmService;
    private final TenantService tenantService;
    private final PmService pmService;

    @Scheduled(fixedRateString = "${open5gs.sync.interval-ms:30000}")
    @SchedulerLock(name = "open5gs_sync", lockAtMostFor = "55s", lockAtLeastFor = "10s")
    public void syncFromOpen5gs() {
        List<Tenant> tenants = tenantService.getActiveTenants();
        if (tenants.isEmpty()) {
            log.debug("No active tenants found, skipping sync");
            return;
        }

        for (Tenant tenant : tenants) {
            try {
                syncGnbInfo(tenant.getTenantId(), tenant.getAmfUrl());
                syncUeInfo(tenant.getTenantId(), tenant.getAmfUrl());
                syncPduInfo(tenant.getTenantId(), tenant.getSmfUrl());
                recordConnectedUeCount(tenant.getTenantId());
                log.debug("Sync completed for tenant {}", tenant.getTenantId());
            } catch (Exception e) {
                log.error("Sync failed for tenant {}", tenant.getTenantId(), e);
                raiseConnectivityAlarm(tenant.getTenantId(), e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void syncGnbInfo(String tenantId, String amfUrl) {
        List<Map<String, Object>> gnbList = open5gsClient.fetchGnbInfo(amfUrl);

        Map<String, GNodeB> existingMap = gNodeBRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(GNodeB::getGnbId, Function.identity(), (a, b) -> a));

        if (gnbList.isEmpty()) {
            log.debug("No gNB data received from AMF InfoAPI for tenant {}", tenantId);
            List<GNodeB> stale = markStaleGnbsDisconnected(existingMap, Collections.emptySet());
            if (!stale.isEmpty()) gNodeBRepository.saveAll(stale);
            return;
        }

        Set<String> syncedGnbIds = new HashSet<>();
        List<GNodeB> toSave = new ArrayList<>();
        for (Map<String, Object> gnbData : gnbList) {
            try {
                String gnbId = extractString(gnbData, "gnb_id");
                if (gnbId == null) continue;
                syncedGnbIds.add(gnbId);

                GNodeB gnb = existingMap.getOrDefault(gnbId, new GNodeB());

                gnb.setTenantId(tenantId);
                gnb.setGnbId(gnbId);
                gnb.setStatus(GNodeB.ConnectionStatus.CONNECTED);
                gnb.setLastSeenAt(System.currentTimeMillis());

                String plmn = extractString(gnbData, "plmn");
                if (plmn != null && plmn.length() >= 5) {
                    GNodeB.PlmnId plmnId = new GNodeB.PlmnId();
                    plmnId.setMcc(plmn.substring(0, 3));
                    plmnId.setMnc(plmn.substring(3));
                    gnb.setSupportedPlmns(List.of(plmnId));
                }

                Map<String, Object> ng = (Map<String, Object>) gnbData.get("ng");
                if (ng != null) {
                    Map<String, Object> sctp = (Map<String, Object>) ng.get("sctp");
                    if (sctp != null) {
                        String peer = extractString(sctp, "peer");
                        if (peer != null) {
                            String ip = peer.replaceAll("[\\[\\]]", "").split(":")[0];
                            gnb.setIpAddress(ip);
                        }
                        GNodeB.SctpInfo sctpInfo = new GNodeB.SctpInfo();
                        sctpInfo.setAddresses(peer != null ? List.of(peer) : Collections.emptyList());
                        Object maxStreams = sctp.get("max_out_streams");
                        if (maxStreams instanceof Number) {
                            sctpInfo.setStreams(((Number) maxStreams).intValue());
                        }
                        gnb.setSctpInfo(sctpInfo);
                    }
                }

                List<Map<String, Object>> taList = (List<Map<String, Object>>) gnbData.get("supported_ta_list");
                if (taList != null) {
                    List<GNodeB.TaiInfo> taiInfos = new ArrayList<>();
                    for (Map<String, Object> ta : taList) {
                        String tacHex = extractString(ta, "tac");
                        if (tacHex != null) {
                            GNodeB.TaiInfo tai = new GNodeB.TaiInfo();
                            try {
                                tai.setTac(Integer.parseInt(tacHex, 16));
                            } catch (NumberFormatException ex) {
                                tai.setTac(0);
                            }
                            List<Map<String, Object>> bplmns = (List<Map<String, Object>>) ta.get("bplmns");
                            if (bplmns != null && !bplmns.isEmpty()) {
                                String bplmn = extractString(bplmns.get(0), "plmn");
                                if (bplmn != null && bplmn.length() >= 5) {
                                    GNodeB.PlmnId taiPlmn = new GNodeB.PlmnId();
                                    taiPlmn.setMcc(bplmn.substring(0, 3));
                                    taiPlmn.setMnc(bplmn.substring(3));
                                    tai.setPlmn(taiPlmn);
                                }
                            }
                            taiInfos.add(tai);
                        }
                    }
                    gnb.setSupportedTais(taiInfos);
                }

                Object ueCount = gnbData.get("num_connected_ues");
                if (ueCount instanceof Number) {
                    gnb.setConnectedUeCount(((Number) ueCount).intValue());
                }

                toSave.add(gnb);
            } catch (Exception e) {
                log.warn("Failed to process gNB data for tenant {}: {}", tenantId, e.getMessage());
            }
        }

        markStaleGnbsDisconnected(existingMap, syncedGnbIds).forEach(toSave::add);
        gNodeBRepository.saveAll(toSave);
        log.debug("Synced {} gNodeBs for tenant {}", gnbList.size(), tenantId);
    }

    @SuppressWarnings("unchecked")
    private void syncUeInfo(String tenantId, String amfUrl) {
        List<Map<String, Object>> ueList = open5gsClient.fetchUeInfo(amfUrl);

        Map<String, ConnectedUe> existingMap = connectedUeRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(ConnectedUe::getImsi, Function.identity(), (a, b) -> a));

        if (ueList.isEmpty()) {
            log.debug("No UE data received from AMF InfoAPI for tenant {}", tenantId);
            List<ConnectedUe> stale = markStaleUesDisconnected(existingMap, Collections.emptySet());
            if (!stale.isEmpty()) connectedUeRepository.saveAll(stale);
            return;
        }

        Set<String> syncedImsis = new HashSet<>();
        List<ConnectedUe> toSave = new ArrayList<>();
        for (Map<String, Object> ueData : ueList) {
            try {
                String supi = extractString(ueData, "supi");
                if (supi == null) continue;
                String imsi = supi.startsWith("imsi-") ? supi.substring(5) : supi;
                syncedImsis.add(imsi);

                ConnectedUe ue = existingMap.getOrDefault(imsi, new ConnectedUe());

                ue.setTenantId(tenantId);
                ue.setImsi(imsi);

                String cmState = extractString(ueData, "cm_state");
                if ("connected".equalsIgnoreCase(cmState)) {
                    ue.setStatus(ConnectedUe.UeStatus.CONNECTED);
                } else if ("idle".equalsIgnoreCase(cmState)) {
                    ue.setStatus(ConnectedUe.UeStatus.IDLE);
                } else {
                    ue.setStatus(ConnectedUe.UeStatus.DISCONNECTED);
                }

                Map<String, Object> gnb = (Map<String, Object>) ueData.get("gnb");
                if (gnb != null) {
                    ue.setGnbId(extractString(gnb, "gnb_id"));
                }

                Map<String, Object> security = (Map<String, Object>) ueData.get("security");
                if (security != null) {
                    ConnectedUe.SecurityInfo secInfo = new ConnectedUe.SecurityInfo();
                    secInfo.setIntegrityAlgorithm(extractString(security, "int"));
                    secInfo.setCipheringAlgorithm(extractString(security, "enc"));
                    ue.setSecurityInfo(secInfo);
                }

                ue.setLastSeenAt(System.currentTimeMillis());
                toSave.add(ue);
            } catch (Exception e) {
                log.warn("Failed to process UE data for tenant {}: {}", tenantId, e.getMessage());
            }
        }

        markStaleUesDisconnected(existingMap, syncedImsis).forEach(toSave::add);
        connectedUeRepository.saveAll(toSave);
        log.debug("Synced {} connected UEs for tenant {}", ueList.size(), tenantId);
    }

    @SuppressWarnings("unchecked")
    private void syncPduInfo(String tenantId, String smfUrl) {
        List<Map<String, Object>> pduList = open5gsClient.fetchPduInfo(smfUrl);

        Map<String, PduSession> existingMap = pduSessionRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(PduSession::getSessionId, Function.identity(), (a, b) -> a));

        if (pduList.isEmpty()) {
            log.debug("No PDU session data received from SMF InfoAPI for tenant {}", tenantId);
            List<PduSession> stale = markStalePduSessionsReleased(existingMap, Collections.emptySet());
            if (!stale.isEmpty()) pduSessionRepository.saveAll(stale);
            return;
        }

        Set<String> syncedSessionIds = new HashSet<>();
        List<PduSession> toSave = new ArrayList<>();
        for (Map<String, Object> ueEntry : pduList) {
            try {
                String supi = extractString(ueEntry, "supi");
                if (supi == null) continue;
                String imsi = supi.startsWith("imsi-") ? supi.substring(5) : supi;

                List<Map<String, Object>> pduSessions = (List<Map<String, Object>>) ueEntry.get("pdu");
                if (pduSessions == null || pduSessions.isEmpty()) continue;

                for (Map<String, Object> pduData : pduSessions) {
                    Object psiObj = pduData.get("psi");
                    String sessionId = imsi + "-psi-" + (psiObj != null ? psiObj : pduData.hashCode());

                    PduSession session = existingMap.getOrDefault(sessionId, new PduSession());

                    session.setTenantId(tenantId);
                    session.setSessionId(sessionId);
                    session.setImsi(imsi);
                    session.setDnn(extractString(pduData, "dnn"));

                    String ipv4 = extractString(pduData, "ipv4");
                    String ipv6 = extractString(pduData, "ipv6");
                    session.setUeIpAddress(ipv4 != null ? ipv4 : ipv6);

                    Map<String, Object> snssai = (Map<String, Object>) pduData.get("snssai");
                    if (snssai != null) {
                        Object sstObj = snssai.get("sst");
                        if (sstObj instanceof Number) session.setSst(((Number) sstObj).intValue());
                        session.setSd(extractString(snssai, "sd"));
                    }

                    String pduState = extractString(pduData, "pdu_state");
                    session.setStatus("active".equalsIgnoreCase(pduState)
                            ? PduSession.SessionStatus.ACTIVE
                            : PduSession.SessionStatus.RELEASED);

                    session.setLastSeenAt(System.currentTimeMillis());
                    toSave.add(session);
                    syncedSessionIds.add(sessionId);
                }
            } catch (Exception e) {
                log.warn("Failed to process PDU session data for tenant {}: {}", tenantId, e.getMessage());
            }
        }

        markStalePduSessionsReleased(existingMap, syncedSessionIds).forEach(toSave::add);
        pduSessionRepository.saveAll(toSave);
        log.debug("Synced PDU sessions from {} UE entries for tenant {}", pduList.size(), tenantId);
    }

    private List<GNodeB> markStaleGnbsDisconnected(Map<String, GNodeB> existingMap, Set<String> activeGnbIds) {
        List<GNodeB> stale = new ArrayList<>();
        for (GNodeB gnb : existingMap.values()) {
            if (gnb.getStatus() == GNodeB.ConnectionStatus.CONNECTED
                    && !activeGnbIds.contains(gnb.getGnbId())) {
                gnb.setStatus(GNodeB.ConnectionStatus.DISCONNECTED);
                gnb.setConnectedUeCount(0);
                stale.add(gnb);
                log.info("Marked gNB {} as DISCONNECTED for tenant {}", gnb.getGnbId(), gnb.getTenantId());
            }
        }
        return stale;
    }

    private List<ConnectedUe> markStaleUesDisconnected(Map<String, ConnectedUe> existingMap, Set<String> activeImsis) {
        List<ConnectedUe> stale = new ArrayList<>();
        for (ConnectedUe ue : existingMap.values()) {
            if (ue.getStatus() != ConnectedUe.UeStatus.DISCONNECTED
                    && !activeImsis.contains(ue.getImsi())) {
                ue.setStatus(ConnectedUe.UeStatus.DISCONNECTED);
                stale.add(ue);
                log.info("Marked UE {} as DISCONNECTED for tenant {}", ue.getImsi(), ue.getTenantId());
            }
        }
        return stale;
    }

    private List<PduSession> markStalePduSessionsReleased(Map<String, PduSession> existingMap, Set<String> activeSessionIds) {
        List<PduSession> stale = new ArrayList<>();
        for (PduSession session : existingMap.values()) {
            if (session.getStatus() == PduSession.SessionStatus.ACTIVE
                    && !activeSessionIds.contains(session.getSessionId())) {
                session.setStatus(PduSession.SessionStatus.RELEASED);
                stale.add(session);
                log.info("Marked PDU session {} as RELEASED for tenant {}", session.getSessionId(), session.getTenantId());
            }
        }
        return stale;
    }

    private void recordConnectedUeCount(String tenantId) {
        long count = connectedUeRepository.countByTenantIdAndStatus(tenantId, ConnectedUe.UeStatus.CONNECTED);
        PmMetric metric = new PmMetric();
        metric.setMetricName("connected_ue_count");
        metric.setValue((double) count);
        metric.setTimestamp(System.currentTimeMillis());
        metric.setMetricType(PmMetric.MetricType.GAUGE);
        pmService.ingestMetric(tenantId, metric);
    }

    private void raiseConnectivityAlarm(String tenantId, String errorMessage) {
        Alarm alarm = new Alarm();
        alarm.setSource("Open5GS-Sync");
        alarm.setAlarmType("CONNECTIVITY_FAILURE");
        alarm.setSeverity(Alarm.Severity.MAJOR);
        alarm.setDescription("Failed to sync with Open5GS: " + errorMessage);
        alarmService.raiseAlarm(tenantId, alarm);
    }

    private String extractString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
}
