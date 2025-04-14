package com.mpjmp.orchestrator.repository;

import com.mpjmp.orchestrator.model.FileEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FileEventRepository extends MongoRepository<FileEvent, String> {
    List<FileEvent> findByFileId(String fileId);
    List<FileEvent> findByEventType(String eventType);
    List<FileEvent> findByStatus(String status);
} 