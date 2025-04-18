package com.mpjmp.orchestrator.repository;

import com.mpjmp.orchestrator.model.SyncRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface SyncRuleRepository extends MongoRepository<SyncRule, String> {
    List<SyncRule> findByDeviceId(String deviceId);
}
