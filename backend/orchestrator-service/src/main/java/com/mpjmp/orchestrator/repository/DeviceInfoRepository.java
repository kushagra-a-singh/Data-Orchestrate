package com.mpjmp.orchestrator.repository;

import com.mpjmp.orchestrator.model.DeviceInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviceInfoRepository extends MongoRepository<DeviceInfo, String> {
    List<DeviceInfo> findByOnline(boolean online);
    List<DeviceInfo> findByStatus(String status);
    List<DeviceInfo> findByDeviceType(String deviceType);
    List<DeviceInfo> findByDeviceNameContainingIgnoreCase(String deviceName);
    List<DeviceInfo> findByLastSeenGreaterThan(String lastSeenIsoString);
}
