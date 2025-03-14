package com.mpjmp.storage.repository;

import com.mpjmp.storage.model.DeviceInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends MongoRepository<DeviceInfo, String> {
}
