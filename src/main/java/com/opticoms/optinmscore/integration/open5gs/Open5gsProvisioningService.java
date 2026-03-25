package com.opticoms.optinmscore.integration.open5gs;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.opticoms.optinmscore.domain.subscriber.model.Subscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Provisions subscribers into the Open5GS MongoDB database.
 * This is the Java equivalent of the Python open5gs.py tool.
 *
 * Open5GS stores subscribers in: database "open5gs", collection "subscribers"
 *
 * Schema matches the format used by Open5GS WebUI and the provided mongodb-tools.
 *
 * Each tenant has its own Open5GS MongoDB identified by open5gsMongoUri.
 * If the URI is null (tenant not yet configured), provisioning is skipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Open5gsProvisioningService {

    private static final String SUBSCRIBERS_COLLECTION = "subscribers";
    private static final int SCHEMA_VERSION = 1;

    private final Open5gsMongoConfig open5gsMongoConfig;

    public void provisionSubscriber(Subscriber subscriber, String open5gsMongoUri) {
        if (open5gsMongoUri == null || open5gsMongoUri.isBlank()) {
            log.warn("Skipping Open5GS provisioning for subscriber {} -- tenant has no open5gsMongoUri configured",
                    subscriber.getImsi());
            return;
        }

        MongoDatabase db = open5gsMongoConfig.getDatabase(open5gsMongoUri);
        MongoCollection<Document> collection = db.getCollection(SUBSCRIBERS_COLLECTION);

        Document existing = collection.find(Filters.eq("imsi", subscriber.getImsi())).first();
        if (existing != null) {
            log.info("Subscriber {} already exists in Open5GS, updating...", subscriber.getImsi());
            collection.replaceOne(
                    Filters.eq("imsi", subscriber.getImsi()),
                    toOpen5gsDocument(subscriber)
            );
        } else {
            Document doc = toOpen5gsDocument(subscriber);
            collection.insertOne(doc);
            log.info("Provisioned subscriber {} into Open5GS", subscriber.getImsi());
        }
    }

    public void deleteSubscriber(String imsi, String open5gsMongoUri) {
        if (open5gsMongoUri == null || open5gsMongoUri.isBlank()) {
            log.warn("Skipping Open5GS delete for subscriber {} -- tenant has no open5gsMongoUri configured", imsi);
            return;
        }

        MongoDatabase db = open5gsMongoConfig.getDatabase(open5gsMongoUri);
        MongoCollection<Document> collection = db.getCollection(SUBSCRIBERS_COLLECTION);
        long deleted = collection.deleteMany(Filters.eq("imsi", imsi)).getDeletedCount();
        log.info("Deleted subscriber {} from Open5GS (count: {})", imsi, deleted);
    }

    public boolean subscriberExists(String imsi, String open5gsMongoUri) {
        if (open5gsMongoUri == null || open5gsMongoUri.isBlank()) {
            log.warn("Cannot check Open5GS subscriber {} -- tenant has no open5gsMongoUri configured", imsi);
            return false;
        }

        MongoDatabase db = open5gsMongoConfig.getDatabase(open5gsMongoUri);
        MongoCollection<Document> collection = db.getCollection(SUBSCRIBERS_COLLECTION);
        return collection.find(Filters.eq("imsi", imsi)).first() != null;
    }

    /**
     * Converts our Subscriber model to the Open5GS MongoDB document format.
     *
     * Target schema (from subscribers.yaml / open5gs WebUI):
     * {
     *   imsi, subscribed_rau_tau_timer, network_access_mode, subscriber_status,
     *   access_restriction_data, slice: [{ sst, sd, default_indicator,
     *     session: [{ name, type, ambr: {uplink: {value,unit}, downlink: {value,unit}},
     *       qos: {index, arp: {priority_level, pre_emption_capability, pre_emption_vulnerability}},
     *       pcc_rule: [] }]
     *   }],
     *   ambr: {uplink: {value,unit}, downlink: {value,unit}},
     *   security: {k, amf, op, opc},
     *   schema_version, __v
     * }
     */
    private Document toOpen5gsDocument(Subscriber subscriber) {
        Document doc = new Document();
        doc.append("imsi", subscriber.getImsi());
        doc.append("subscribed_rau_tau_timer", 12);
        doc.append("network_access_mode", 0);
        doc.append("subscriber_status", 0);
        doc.append("access_restriction_data", 32);

        List<Document> slices = new ArrayList<>();
        if (subscriber.getProfileList() != null) {
            for (int i = 0; i < subscriber.getProfileList().size(); i++) {
                Subscriber.SessionProfile profile = subscriber.getProfileList().get(i);
                Document slice = buildSliceDocument(profile, i == 0);
                slices.add(slice);
            }
        }
        doc.append("slice", slices);

        doc.append("ambr", new Document()
                .append("uplink", toAmbrDocument(subscriber.getUeAmbrUl()))
                .append("downlink", toAmbrDocument(subscriber.getUeAmbrDl()))
        );

        Document security = new Document();
        security.append("k", subscriber.getKi());
        security.append("amf", "8000");
        if (subscriber.getUsimType() == Subscriber.UsimType.OPC) {
            security.append("opc", subscriber.getOpc());
            security.append("op", null);
        } else {
            security.append("op", subscriber.getOp());
            security.append("opc", null);
        }
        doc.append("security", security);

        doc.append("schema_version", SCHEMA_VERSION);
        doc.append("__v", 0);

        return doc;
    }

    private Document buildSliceDocument(Subscriber.SessionProfile profile, boolean isDefault) {
        Document session = new Document();
        session.append("name", profile.getApnDnn());
        session.append("type", profile.getPduType());
        session.append("pcc_rule", new ArrayList<>());
        session.append("ambr", new Document()
                .append("uplink", toAmbrDocument(profile.getSessionAmbrUl()))
                .append("downlink", toAmbrDocument(profile.getSessionAmbrDl()))
        );
        session.append("qos", new Document()
                .append("index", profile.getQi5g())
                .append("arp", new Document()
                        .append("priority_level", profile.getArpPriority())
                        .append("pre_emption_capability", profile.isPreemptionCapability() ? 1 : 2)
                        .append("pre_emption_vulnerability", profile.isPreemptionVulnerability() ? 1 : 2)
                )
        );

        Document slice = new Document();
        slice.append("sst", profile.getSst());
        slice.append("sd", profile.getSd());
        slice.append("default_indicator", isDefault);
        slice.append("session", List.of(session));

        return slice;
    }

    /**
     * Converts bps value to Open5GS AMBR format {value, unit}.
     * Open5GS units: 0=bps, 1=Kbps, 2=Mbps, 3=Gbps, 4=Tbps
     */
    private Document toAmbrDocument(long bps) {
        if (bps <= 0) {
            return new Document().append("value", 1).append("unit", 3);
        }

        long tbps = 1_000_000_000_000L;
        long gbps = 1_000_000_000L;
        long mbps = 1_000_000L;
        long kbps = 1_000L;

        if (bps >= tbps && bps % tbps == 0) {
            return new Document().append("value", (int) (bps / tbps)).append("unit", 4);
        } else if (bps >= gbps && bps % gbps == 0) {
            return new Document().append("value", (int) (bps / gbps)).append("unit", 3);
        } else if (bps >= mbps && bps % mbps == 0) {
            return new Document().append("value", (int) (bps / mbps)).append("unit", 2);
        } else if (bps >= kbps && bps % kbps == 0) {
            return new Document().append("value", (int) (bps / kbps)).append("unit", 1);
        } else {
            return new Document().append("value", (int) bps).append("unit", 0);
        }
    }
}
