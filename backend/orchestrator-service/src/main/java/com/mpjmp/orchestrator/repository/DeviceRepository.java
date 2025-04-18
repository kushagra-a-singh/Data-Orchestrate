package com.mpjmp.orchestrator.repository;

import com.mpjmp.orchestrator.model.DeviceInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DeviceRepository extends MongoRepository<DeviceInfo, String> {
    List<DeviceInfo> findByOnlineTrue();
    List<DeviceInfo> findByOnlineFalse();
}
