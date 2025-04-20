package com.mpjmp.orchestrator.service;

import com.mpjmp.orchestrator.model.BulkOperation;
import com.mpjmp.orchestrator.model.BulkOperationStatus;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BulkOperationService {
    private final MongoTemplate mongoTemplate;
    
    public String startBulkOperation(List<String> fileIds) {
        String batchId = UUID.randomUUID().toString();
        mongoTemplate.insert(new BulkOperation(batchId, fileIds));
        return batchId;
    }
    
    public BulkOperationStatus getStatus(String batchId) {
        return mongoTemplate.findById(batchId, BulkOperationStatus.class);
    }
}
