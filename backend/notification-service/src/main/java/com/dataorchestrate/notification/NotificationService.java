package com.dataorchestrate.notification;

import com.dataorchestrate.common.DeviceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final DeviceIdentifier deviceIdentifier;
    
    @Autowired
    public NotificationService(DeviceIdentifier deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
        logger.info("Initializing NotificationService for device: {}", deviceIdentifier.getDeviceId());
    }
    
    @KafkaListener(topics = "file.processed", groupId = "notification-group")
    public void handleFileProcessed(String message) {
        String deviceId = deviceIdentifier.getDeviceId();
        logger.info("Device {} received notification: {}", deviceId, message);
        // Handle notification (e.g., send to UI, store in database)
    }
    
    @KafkaListener(topics = "file.uploaded", groupId = "notification-group")
    public void handleFileUploaded(String message) {
        String deviceId = deviceIdentifier.getDeviceId();
        logger.info("Device {} received upload notification: {}", deviceId, message);
        // Handle upload notification
    }
    
    @KafkaListener(topics = "file.synced", groupId = "notification-group")
    public void handleFileSynced(String message) {
        String deviceId = deviceIdentifier.getDeviceId();
        logger.info("Device {} received sync notification: {}", deviceId, message);
        // Handle sync notification
    }
} 