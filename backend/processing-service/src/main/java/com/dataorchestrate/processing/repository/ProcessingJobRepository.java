package com.dataorchestrate.processing.repository;

import com.dataorchestrate.processing.model.ProcessingJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessingJobRepository extends MongoRepository<ProcessingJob, String> {
    Optional<ProcessingJob> findByFileId(String fileId);
    List<ProcessingJob> findByStatus(String status);
    List<ProcessingJob> findByStatusAndStartedAtBefore(String status, LocalDateTime dateTime);
}