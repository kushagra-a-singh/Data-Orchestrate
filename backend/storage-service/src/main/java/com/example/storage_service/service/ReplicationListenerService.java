package com.example.storage_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplicationListenerService {

    @KafkaListener(topics = "${kafka.topic.file-replication}", groupId = "storage-group")
    public void handleFileReplication(String message) {
        log.info("Received file replication request: {}", message);
        // TODO: Implement file replication logic
    }
}