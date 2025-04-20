package com.dataorchestrate.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class NotificationSender {
    private static final String NOTIFICATION_URL = "http://localhost:8087/notifications"; // Adjust port if needed
    private static final RestTemplate restTemplate = new RestTemplate();

    public static void sendNotification(String type, String title, String message, Double progress, String fileId, String device, String location) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        if (progress != null) notification.put("progress", progress);
        if (fileId != null) notification.put("fileId", fileId);
        if (device != null) notification.put("device", device);
        if (location != null) notification.put("location", location);
        notification.put("timestamp", Instant.now().toString());
        try {
            restTemplate.postForEntity(NOTIFICATION_URL, notification, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
