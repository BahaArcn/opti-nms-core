package com.opticoms.optinmscore.domain.observability.config;

import com.opticoms.optinmscore.domain.observability.model.Alarm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AlarmIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void ensureAlarmIndexes() {
        IndexDefinition index = new CompoundIndexDefinition(
                new Document("tenantId", 1).append("source", 1).append("alarmType", 1))
                .named("active_alarm_dedup_unique_idx")
                .unique()
                .partial(PartialIndexFilter.of(Criteria.where("status").is("ACTIVE")));

        mongoTemplate.indexOps(Alarm.class).ensureIndex(index);
        log.info("Alarm dedup partial unique index ensured");
    }
}
