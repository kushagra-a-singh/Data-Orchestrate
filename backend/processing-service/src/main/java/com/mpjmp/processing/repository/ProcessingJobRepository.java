package com.mpjmp.processing.repository;

import com.mpjmp.processing.model.ProcessingJob;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProcessingJobRepository extends MongoRepository<ProcessingJob, String> {
    List<ProcessingJob> findByFileId(String fileId);
    List<ProcessingJob> findByStatus(String status);
    List<ProcessingJob> findByStatusAndStartedAtBefore(String status, LocalDateTime dateTime);
} 