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
    
    @KafkaListener(topics = {"file.uploaded", "file.processed", "file.synced"}, groupId = "notification-group")
    public void handleNotification(String message) {
        String deviceId = deviceIdentifier.getDeviceId();
        logger.info("Device {} received notification: {}", deviceId, message);
        // Forward notification to GUI (WebSocket or REST call)
        // Add your notification forwarding logic here
    }
} 