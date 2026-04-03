package com.opticoms.optinmscore.domain.subscriber.importer;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvSubscriberParserTest {

    private final CsvSubscriberParser parser = new CsvSubscriberParser();

    @Test
    void parse_validCsv_returnsSubscribers() throws IOException {
        String csv = """
                imsi,ki,usimType,opc,dnn,sst,ueAmbrDl,ueAmbrUl
                286010000000001,465B5CE8B199B49FAA5F0A2EE238A6BC,OPC,E8ED289DEBA952E4283B54E88E6183CA,internet,1,1000000000,500000000
                286010000000002,465B5CE8B199B49FAA5F0A2EE238A6BC,OPC,E8ED289DEBA952E4283B54E88E6183CA,ims,2,500000000,250000000
                """;

        List<Subscriber> result = parser.parse(toStream(csv));

        assertEquals(2, result.size());
        assertEquals("286010000000001", result.get(0).getImsi());
        assertEquals("internet", result.get(0).getProfileList().get(0).getApnDnn());
        assertEquals(1, result.get(0).getProfileList().get(0).getSst());
        assertEquals("286010000000002", result.get(1).getImsi());
        assertEquals("ims", result.get(1).getProfileList().get(0).getApnDnn());
    }

    @Test
    void parse_quotedFieldsWithCommas_handledCorrectly() throws IOException {
        String csv = """
                imsi,ki,usimType,opc,dnn,label
                286010000000001,465B5CE8B199B49FAA5F0A2EE238A6BC,OPC,E8ED289DEBA952E4283B54E88E6183CA,internet,"Baha's Phone, v2"
                """;

        List<Subscriber> result = parser.parse(toStream(csv));

        assertEquals(1, result.size());
        assertEquals("Baha's Phone, v2", result.get(0).getLabel());
    }

    @Test
    void parse_emptyFile_returnsEmptyList() throws IOException {
        List<Subscriber> result = parser.parse(toStream(""));
        assertTrue(result.isEmpty());
    }

    @Test
    void parse_headerOnly_returnsEmptyList() throws IOException {
        List<Subscriber> result = parser.parse(toStream("imsi,ki,usimType,opc,dnn"));
        assertTrue(result.isEmpty());
    }

    @Test
    void supports_csvExtension_returnsTrue() {
        assertTrue(parser.supports(null, "subscribers.csv"));
        assertTrue(parser.supports(null, "DATA.CSV"));
    }

    @Test
    void supports_csvContentType_returnsTrue() {
        assertTrue(parser.supports("text/csv", "file.txt"));
    }

    @Test
    void supports_otherFormat_returnsFalse() {
        assertFalse(parser.supports("application/json", "file.json"));
        assertFalse(parser.supports(null, "file.xlsx"));
    }

    @Test
    void parseCsvLine_static_handlesEscapedQuotes() {
        String[] result = CsvSubscriberParser.parseCsvLine("a,\"b\"\"c\",d");
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b\"c", result[1]);
        assertEquals("d", result[2]);
    }

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
