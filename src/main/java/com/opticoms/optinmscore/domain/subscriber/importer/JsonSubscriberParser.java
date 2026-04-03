package com.opticoms.optinmscore.domain.subscriber.importer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class JsonSubscriberParser implements SubscriberImportParser {

    private final ObjectMapper objectMapper;

    public JsonSubscriberParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public List<Subscriber> parse(InputStream inputStream) throws IOException {
        return objectMapper.readValue(inputStream, new TypeReference<>() {});
    }

    @Override
    public boolean supports(String contentType, String filename) {
        if (filename != null && filename.toLowerCase().endsWith(".json")) {
            return true;
        }
        return "application/json".equalsIgnoreCase(contentType);
    }
}
