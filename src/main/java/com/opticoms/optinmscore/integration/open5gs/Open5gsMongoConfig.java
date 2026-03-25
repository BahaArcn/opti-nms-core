package com.opticoms.optinmscore.integration.open5gs;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tenant MongoDB connection cache for Open5GS subscriber provisioning.
 *
 * Each tenant has its own Open5GS instance with its own MongoDB.
 * Connections are lazily created and cached by URI.
 * MongoClient is thread-safe; ConcurrentHashMap.computeIfAbsent is atomic.
 */
@Slf4j
@Configuration
public class Open5gsMongoConfig {

    private final ConcurrentHashMap<String, MongoClient> clientCache = new ConcurrentHashMap<>();

    public MongoDatabase getDatabase(String mongoUri) {
        MongoClient client = clientCache.computeIfAbsent(mongoUri, uri -> {
            String maskedUri = uri.replaceAll("(mongodb://)[^@]*@", "$1*****@");
            log.info("Creating Open5GS MongoDB connection: {}", maskedUri);
            return MongoClients.create(uri);
        });
        String dbName = extractDatabaseName(mongoUri);
        return client.getDatabase(dbName);
    }

    @PreDestroy
    public void close() {
        clientCache.forEach((uri, client) -> {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Error closing Open5GS MongoClient for {}: {}", uri, e.getMessage());
            }
        });
        clientCache.clear();
    }

    private String extractDatabaseName(String uri) {
        String withoutParams = uri.split("\\?")[0];
        String afterSlash = withoutParams.substring(withoutParams.lastIndexOf('/') + 1);
        return afterSlash.isEmpty() ? "open5gs" : afterSlash;
    }
}
