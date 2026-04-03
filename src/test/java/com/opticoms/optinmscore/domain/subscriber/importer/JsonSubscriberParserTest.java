package com.opticoms.optinmscore.domain.subscriber.importer;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonSubscriberParserTest {

    private final JsonSubscriberParser parser = new JsonSubscriberParser();

    @Test
    void parse_validArray_returnsSubscribers() throws IOException {
        String json = """
                [
                  {
                    "imsi": "286010000000001",
                    "ki": "465B5CE8B199B49FAA5F0A2EE238A6BC",
                    "usimType": "OPC",
                    "opc": "E8ED289DEBA952E4283B54E88E6183CA",
                    "ueAmbrDl": 1000000000,
                    "ueAmbrUl": 500000000,
                    "profileList": [
                      { "apnDnn": "internet", "sst": 1 }
                    ]
                  },
                  {
                    "imsi": "286010000000002",
                    "ki": "465B5CE8B199B49FAA5F0A2EE238A6BC",
                    "usimType": "OPC",
                    "opc": "E8ED289DEBA952E4283B54E88E6183CA",
                    "profileList": [
                      { "apnDnn": "ims", "sst": 1 },
                      { "apnDnn": "internet", "sst": 2 }
                    ]
                  }
                ]
                """;

        List<Subscriber> result = parser.parse(toStream(json));

        assertEquals(2, result.size());
        assertEquals("286010000000001", result.get(0).getImsi());
        assertEquals(Subscriber.UsimType.OPC, result.get(0).getUsimType());
        assertEquals(1, result.get(0).getProfileList().size());
        assertEquals(2, result.get(1).getProfileList().size());
    }

    @Test
    void parse_emptyArray_returnsEmptyList() throws IOException {
        List<Subscriber> result = parser.parse(toStream("[]"));
        assertTrue(result.isEmpty());
    }

    @Test
    void parse_invalidJson_throwsIOException() {
        assertThrows(IOException.class, () -> parser.parse(toStream("not json")));
    }

    @Test
    void supports_jsonExtension_returnsTrue() {
        assertTrue(parser.supports(null, "subscribers.json"));
        assertTrue(parser.supports(null, "DATA.JSON"));
    }

    @Test
    void supports_jsonContentType_returnsTrue() {
        assertTrue(parser.supports("application/json", "file.txt"));
    }

    @Test
    void supports_otherFormat_returnsFalse() {
        assertFalse(parser.supports("text/csv", "file.csv"));
        assertFalse(parser.supports(null, "file.xlsx"));
    }

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
