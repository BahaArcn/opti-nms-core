package com.opticoms.optinmscore.domain.subscriber.importer;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelSubscriberParserTest {

    private final ExcelSubscriberParser parser = new ExcelSubscriberParser();

    @Test
    void parse_validExcel_returnsSubscribers() throws IOException {
        byte[] excelBytes = createTestExcel(
                new String[]{"imsi", "ki", "usimType", "opc", "dnn", "sst", "ueAmbrDl", "ueAmbrUl"},
                new Object[][]{
                        {"286010000000001", "465B5CE8B199B49FAA5F0A2EE238A6BC", "OPC",
                                "E8ED289DEBA952E4283B54E88E6183CA", "internet", 1, 1000000000L, 500000000L},
                        {"286010000000002", "465B5CE8B199B49FAA5F0A2EE238A6BC", "OPC",
                                "E8ED289DEBA952E4283B54E88E6183CA", "ims", 2, 500000000L, 250000000L}
                }
        );

        List<Subscriber> result = parser.parse(new ByteArrayInputStream(excelBytes));

        assertEquals(2, result.size());
        assertEquals("286010000000001", result.get(0).getImsi());
        assertEquals("internet", result.get(0).getProfileList().get(0).getApnDnn());
        assertEquals(1, result.get(0).getProfileList().get(0).getSst());
        assertEquals("286010000000002", result.get(1).getImsi());
    }

    @Test
    void parse_missingColumns_subscriberHasNullFields() throws IOException {
        byte[] excelBytes = createTestExcel(
                new String[]{"imsi", "dnn"},
                new Object[][]{
                        {"286010000000001", "internet"}
                }
        );

        List<Subscriber> result = parser.parse(new ByteArrayInputStream(excelBytes));

        assertEquals(1, result.size());
        assertNull(result.get(0).getKi());
        assertNull(result.get(0).getUsimType());
        assertEquals("internet", result.get(0).getProfileList().get(0).getApnDnn());
    }

    @Test
    void parse_emptyExcel_returnsEmptyList() throws IOException {
        byte[] excelBytes;
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("Sheet1");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            excelBytes = out.toByteArray();
        }

        List<Subscriber> result = parser.parse(new ByteArrayInputStream(excelBytes));
        assertTrue(result.isEmpty());
    }

    @Test
    void parse_headerOnly_returnsEmptyList() throws IOException {
        byte[] excelBytes = createTestExcel(
                new String[]{"imsi", "ki", "usimType"},
                new Object[][]{}
        );

        List<Subscriber> result = parser.parse(new ByteArrayInputStream(excelBytes));
        assertTrue(result.isEmpty());
    }

    @Test
    void supports_xlsxExtension_returnsTrue() {
        assertTrue(parser.supports(null, "subscribers.xlsx"));
        assertTrue(parser.supports(null, "DATA.XLSX"));
    }

    @Test
    void supports_xlsxContentType_returnsTrue() {
        assertTrue(parser.supports(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "file.bin"));
    }

    @Test
    void supports_otherFormat_returnsFalse() {
        assertFalse(parser.supports("text/csv", "file.csv"));
        assertFalse(parser.supports(null, "file.json"));
    }

    private byte[] createTestExcel(String[] headers, Object[][] data) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Subscribers");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    Object val = data[r][c];
                    if (val instanceof String s) {
                        row.createCell(c).setCellValue(s);
                    } else if (val instanceof Number n) {
                        row.createCell(c).setCellValue(n.doubleValue());
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
