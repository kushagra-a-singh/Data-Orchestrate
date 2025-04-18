package com.dataorchestrate.notification;

import com.dataorchestrate.common.DeviceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final DeviceIdentifier deviceIdentifier;
    private final RestTemplate restTemplate;

    @Autowired
    public NotificationService(DeviceIdentifier deviceIdentifier, RestTemplate restTemplate) {
        this.deviceIdentifier = deviceIdentifier;
        this.restTemplate = restTemplate;
        logger.info("Initializing NotificationService for device: {}", deviceIdentifier.getDeviceId());
    }

    @PostMapping("/notifications")
    public ResponseEntity<String> handleNotification(@RequestBody String message) {
        String deviceId = deviceIdentifier.getDeviceId();
        logger.info("Device {} received notification: {}", deviceId, message);
        // Forward notification to GUI (WebSocket or REST call)
        // Add your notification forwarding logic here
        return ResponseEntity.ok("Notification received");
    }

    // Alternatively, you can use polling to fetch notifications
    // public void pollNotifications() {
    //     String url = "https://example.com/notifications";
    //     String response = restTemplate.getForObject(url, String.class);
    //     // Process the response
    // }
}