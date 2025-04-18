package com.example.storage_service.repository;

import com.example.storage_service.model.SyncRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface SyncRuleRepository extends MongoRepository<SyncRule, String> {
    List<SyncRule> findByDeviceId(String deviceId);
    void deleteByDeviceId(String deviceId);
}
