package com.example.notification_service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Date;
import com.example.notification_service.model.Notification;

@Service
@RestController
@RequestMapping("/api/notifications")
public class FileNotificationService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @KafkaListener(
        topics = "file-replication-topic",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleNotification(String fileId, String fileContent, String userId, String userIp) {
        Notification notification = new Notification(
            fileId,
            "File Replication",
            "File " + fileId + " has been replicated from " + userIp,
            userId,
            false
        );
        
        mongoTemplate.save(notification, "notifications");
        System.out.println("Notification stored for file: " + fileId);
    }
    
    @GetMapping("/user/{userId}")
    public List<Notification> getUserNotifications(@PathVariable String userId) {
        return mongoTemplate.find(
            org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("userId").is(userId)
                    .and("read").is(false)
            ),
            Notification.class,
            "notifications"
        );
    }
    
    @PutMapping("/{notificationId}/read")
    public void markAsRead(@PathVariable String notificationId) {
        Notification notification = mongoTemplate.findById(notificationId, Notification.class, "notifications");
        if (notification != null) {
            notification.setRead(true);
            mongoTemplate.save(notification, "notifications");
        }
    }
}
