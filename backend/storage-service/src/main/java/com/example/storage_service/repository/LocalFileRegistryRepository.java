package com.example.storage_service.repository;

import com.example.storage_service.model.LocalFileRegistry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface LocalFileRegistryRepository extends MongoRepository<LocalFileRegistry, String> {
    
    List<LocalFileRegistry> findByDeviceId(String deviceId);
    
    Optional<LocalFileRegistry> findByDeviceIdAndFileId(String deviceId, String fileId);
    
    @Query("{ 'deviceId': ?0, 'verified': true }")
    List<LocalFileRegistry> findVerifiedFilesByDevice(String deviceId);
    
    @Query("{ 'fileId': ?0, 'verified': true }")
    List<LocalFileRegistry> findDevicesWithVerifiedFile(String fileId);
    
    @Query("{ 'verified': false, 'verificationAttempts': { $lt: ?0 }}")
    List<LocalFileRegistry> findUnverifiedFilesWithinAttempts(int maxAttempts);
} 