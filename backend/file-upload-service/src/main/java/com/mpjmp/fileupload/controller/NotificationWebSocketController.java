package com.mpjmp.fileupload.controller;

import com.mpjmp.fileupload.kafka.ReplicationEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationWebSocketController {
    private final SimpMessagingTemplate template;
    public NotificationWebSocketController(SimpMessagingTemplate template) {
        this.template = template;
    }
    public void sendReplicationNotification(ReplicationEvent event) {
        template.convertAndSend("/topic/replication-status", event);
    }
    public void sendLog(String log) {
        template.convertAndSend("/topic/logs", log);
    }
    public void sendNotification(String notification) {
        template.convertAndSend("/topic/notifications", notification);
    }
}
