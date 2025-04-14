package com.example.storage_service.repository;

import com.example.storage_service.model.DeviceInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends MongoRepository<DeviceInfo, String> {
} 