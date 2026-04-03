package com.opticoms.optinmscore.domain.subscriber.importer;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class CsvSubscriberParser implements SubscriberImportParser {

    @Override
    public List<Subscriber> parse(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return List.of();
            }
            headerLine = headerLine.replace("\uFEFF", "");

            Map<String, Integer> headerMap = parseHeader(headerLine);
            List<Subscriber> subscribers = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] fields = parseCsvLine(line);
                Subscriber sub = mapFieldsToSubscriber(fields, headerMap);
                if (sub != null) {
                    subscribers.add(sub);
                }
            }

            return subscribers;
        }
    }

    @Override
    public boolean supports(String contentType, String filename) {
        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            return true;
        }
        return "text/csv".equalsIgnoreCase(contentType);
    }

    private Map<String, Integer> parseHeader(String headerLine) {
        String[] headers = parseCsvLine(headerLine);
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String name = headers[i].trim().toLowerCase();
            if (!name.isEmpty()) {
                map.put(name, i);
            }
        }
        return map;
    }

    private Subscriber mapFieldsToSubscriber(String[] fields, Map<String, Integer> headerMap) {
        Subscriber sub = new Subscriber();

        sub.setImsi(getField(fields, headerMap, "imsi"));
        sub.setMsisdn(getField(fields, headerMap, "msisdn"));
        sub.setLabel(getField(fields, headerMap, "label"));
        sub.setKi(getField(fields, headerMap, "ki"));
        sub.setOpc(getField(fields, headerMap, "opc"));
        sub.setOp(getField(fields, headerMap, "op"));
        sub.setSqn(getField(fields, headerMap, "sqn"));
        sub.setPolicyId(getField(fields, headerMap, "policyid"));
        sub.setEdgeLocationId(getField(fields, headerMap, "edgelocationid"));

        String usimTypeStr = getField(fields, headerMap, "usimtype");
        if (usimTypeStr != null && !usimTypeStr.isEmpty()) {
            try {
                sub.setUsimType(Subscriber.UsimType.valueOf(usimTypeStr.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        String simTypeStr = getField(fields, headerMap, "simtype");
        if (simTypeStr != null && !simTypeStr.isEmpty()) {
            try {
                sub.setSimType(Subscriber.SimType.valueOf(simTypeStr.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        sub.setUeAmbrDl(getLongField(fields, headerMap, "ueambrdl"));
        sub.setUeAmbrUl(getLongField(fields, headerMap, "ueambrul"));

        String lboStr = getField(fields, headerMap, "lboroamingallowed");
        if ("true".equalsIgnoreCase(lboStr) || "1".equals(lboStr)) {
            sub.setLboRoamingAllowed(true);
        }

        Subscriber.SessionProfile profile = new Subscriber.SessionProfile();
        String dnn = getField(fields, headerMap, "dnn");
        if (dnn == null || dnn.isEmpty()) {
            dnn = getField(fields, headerMap, "apndnn");
        }
        profile.setApnDnn(dnn);
        profile.setSst((int) getLongField(fields, headerMap, "sst"));
        profile.setSd(getField(fields, headerMap, "sd"));
        profile.setPduType((int) getLongField(fields, headerMap, "pdutype"));
        profile.setQi5g((int) getLongField(fields, headerMap, "qi5g"));
        profile.setQci4g((int) getLongField(fields, headerMap, "qci4g"));
        profile.setArpPriority((int) getLongField(fields, headerMap, "arppriority"));
        profile.setSessionAmbrDl(getLongField(fields, headerMap, "sessionambrdl"));
        profile.setSessionAmbrUl(getLongField(fields, headerMap, "sessionambrul"));

        sub.setProfileList(List.of(profile));

        return sub;
    }

    private String getField(String[] fields, Map<String, Integer> headerMap, String fieldName) {
        Integer idx = headerMap.get(fieldName);
        if (idx == null || idx >= fields.length) return null;
        String val = fields[idx].trim();
        return val.isEmpty() ? null : val;
    }

    private long getLongField(String[] fields, Map<String, Integer> headerMap, String fieldName) {
        String val = getField(fields, headerMap, fieldName);
        if (val == null) return 0;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parses a CSV line respecting quoted fields (handles commas inside double quotes).
     */
    static String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    tokens.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        tokens.add(current.toString());

        return tokens.toArray(new String[0]);
    }
}
