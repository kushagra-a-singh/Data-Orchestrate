package com.mpjmp.orchestrator.kafka;

import com.mpjmp.orchestrator.service.FileEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileEventConsumer {

    private final FileEventService fileEventService;

    @KafkaListener(topics = "${kafka.topic.file-upload}", groupId = "orchestrator-group")
    public void handleFileUpload(String message) {
        log.info("Received file upload event: {}", message);
        fileEventService.handleFileUpload(message);
    }

    @KafkaListener(topics = "${kafka.topic.file-deleted}", groupId = "orchestrator-group")
    public void handleFileDeletion(String message) {
        log.info("Received file deletion event: {}", message);
        fileEventService.handleFileDeletion(message);
    }

    @KafkaListener(topics = "${kafka.topic.file-status}", groupId = "orchestrator-group")
    public void handleFileStatusChange(String message) {
        log.info("Received file status change event: {}", message);
        fileEventService.handleFileStatusChange(message);
    }
} 