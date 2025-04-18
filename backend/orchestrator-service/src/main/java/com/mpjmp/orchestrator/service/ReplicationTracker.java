package com.mpjmp.orchestrator.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import java.time.Instant;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import com.mpjmp.orchestrator.model.ReplicationProgress;

@Service
@RequiredArgsConstructor
public class ReplicationTracker {
    private final MongoTemplate mongoTemplate;
    
    public void trackProgress(String fileId, String deviceId, double progress) {
        mongoTemplate.upsert(
            query(where("fileId").is(fileId).and("deviceId").is(deviceId)),
            new Update()
                .set("progress", progress)
                .set("lastUpdated", Instant.now()),
            ReplicationProgress.class
        );
    }
    
    public ReplicationProgress getProgress(String fileId, String deviceId) {
        return mongoTemplate.findOne(
            query(where("fileId").is(fileId).and("deviceId").is(deviceId)), 
            ReplicationProgress.class
        );
    }
}
