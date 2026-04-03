package com.opticoms.optinmscore.domain.subscriber.importer;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class ExcelSubscriberParser implements SubscriberImportParser {

    @Override
    public List<Subscriber> parse(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                return List.of();
            }

            Map<String, Integer> headerMap = parseHeader(sheet.getRow(0));
            List<Subscriber> subscribers = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                Subscriber sub = mapRowToSubscriber(row, headerMap);
                if (sub != null) {
                    subscribers.add(sub);
                }
            }

            return subscribers;
        }
    }

    @Override
    public boolean supports(String contentType, String filename) {
        if (filename != null && filename.toLowerCase().endsWith(".xlsx")) {
            return true;
        }
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equalsIgnoreCase(contentType);
    }

    private Map<String, Integer> parseHeader(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) return map;
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String name = getCellStringValue(cell).trim().toLowerCase();
                if (!name.isEmpty()) {
                    map.put(name, i);
                }
            }
        }
        return map;
    }

    private Subscriber mapRowToSubscriber(Row row, Map<String, Integer> headerMap) {
        Subscriber sub = new Subscriber();

        sub.setImsi(getStringField(row, headerMap, "imsi"));
        sub.setMsisdn(getStringField(row, headerMap, "msisdn"));
        sub.setLabel(getStringField(row, headerMap, "label"));
        sub.setKi(getStringField(row, headerMap, "ki"));
        sub.setOpc(getStringField(row, headerMap, "opc"));
        sub.setOp(getStringField(row, headerMap, "op"));
        sub.setSqn(getStringField(row, headerMap, "sqn"));
        sub.setPolicyId(getStringField(row, headerMap, "policyid"));
        sub.setEdgeLocationId(getStringField(row, headerMap, "edgelocationid"));

        String usimTypeStr = getStringField(row, headerMap, "usimtype");
        if (usimTypeStr != null && !usimTypeStr.isEmpty()) {
            try {
                sub.setUsimType(Subscriber.UsimType.valueOf(usimTypeStr.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // validation will catch this later
            }
        }

        String simTypeStr = getStringField(row, headerMap, "simtype");
        if (simTypeStr != null && !simTypeStr.isEmpty()) {
            try {
                sub.setSimType(Subscriber.SimType.valueOf(simTypeStr.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // validation will catch this later
            }
        }

        sub.setUeAmbrDl(getLongField(row, headerMap, "ueambrdl"));
        sub.setUeAmbrUl(getLongField(row, headerMap, "ueambrul"));

        String lboStr = getStringField(row, headerMap, "lboroamingallowed");
        if ("true".equalsIgnoreCase(lboStr) || "1".equals(lboStr)) {
            sub.setLboRoamingAllowed(true);
        }

        Subscriber.SessionProfile profile = new Subscriber.SessionProfile();
        String dnn = getStringField(row, headerMap, "dnn");
        if (dnn == null || dnn.isEmpty()) {
            dnn = getStringField(row, headerMap, "apndnn");
        }
        profile.setApnDnn(dnn);
        profile.setSst((int) getLongField(row, headerMap, "sst"));
        profile.setSd(getStringField(row, headerMap, "sd"));
        profile.setPduType((int) getLongField(row, headerMap, "pdutype"));
        profile.setQi5g((int) getLongField(row, headerMap, "qi5g"));
        profile.setQci4g((int) getLongField(row, headerMap, "qci4g"));
        profile.setArpPriority((int) getLongField(row, headerMap, "arppriority"));
        profile.setSessionAmbrDl(getLongField(row, headerMap, "sessionambrdl"));
        profile.setSessionAmbrUl(getLongField(row, headerMap, "sessionambrul"));

        sub.setProfileList(List.of(profile));

        return sub;
    }

    private String getStringField(Row row, Map<String, Integer> headerMap, String fieldName) {
        Integer idx = headerMap.get(fieldName);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        String val = getCellStringValue(cell).trim();
        return val.isEmpty() ? null : val;
    }

    private long getLongField(Row row, Map<String, Integer> headerMap, String fieldName) {
        Integer idx = headerMap.get(fieldName);
        if (idx == null) return 0;
        Cell cell = row.getCell(idx);
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }
        String val = getCellStringValue(cell).trim();
        if (val.isEmpty()) return 0;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getStringCellValue();
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell).trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }
}
