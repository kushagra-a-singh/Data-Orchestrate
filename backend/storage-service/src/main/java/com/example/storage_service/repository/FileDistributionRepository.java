package com.example.storage_service.repository;

import com.example.storage_service.model.FileDistribution;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;

public interface FileDistributionRepository extends MongoRepository<FileDistribution, String> {
    
    @Query("{ 'pendingDevices': { $in: [?0] }}")
    List<FileDistribution> findByPendingDevice(String deviceId);
    
    @Query("{ 'availableOnDevices': { $in: [?0] }}")
    List<FileDistribution> findByAvailableDevice(String deviceId);
    
    @Query("{ 'pendingDevices': { $ne: [] }}")
    List<FileDistribution> findAllWithPendingReplications();
    
    @Query("{ 'availableOnDevices': ?0, 'pendingDevices': { $ne: [] }}")
    List<FileDistribution> findPendingReplicationsForDevice(String deviceId);
} 