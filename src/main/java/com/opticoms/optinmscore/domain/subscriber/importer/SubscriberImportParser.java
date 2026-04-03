package com.opticoms.optinmscore.domain.subscriber.importer;

import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface SubscriberImportParser {

    List<Subscriber> parse(InputStream inputStream) throws IOException;

    boolean supports(String contentType, String filename);
}
