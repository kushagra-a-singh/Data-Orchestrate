package com.dataorchestrate.common;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for Device entity
 */
@Repository
public interface DeviceRepository extends MongoRepository<Device, String> {
    
    /**
     * Find a device by its MAC address
     * @param macAddress The MAC address to search for
     * @return An Optional containing the device if found
     */
    Optional<Device> findByMacAddress(String macAddress);
    
    /**
     * Find a device by its hostname/device name
     * @param deviceName The device name to search for
     * @return An Optional containing the device if found
     */
    Optional<Device> findByDeviceName(String deviceName);
}
