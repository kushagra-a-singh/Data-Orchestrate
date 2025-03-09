package com.mpjmp.orchestrator.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorListener {

    @KafkaListener(topics = "file-upload-topic", groupId = "orchestrator-group")
    public void listenFileUploads(String message) {
        System.out.println("Orchestrator received file: " + message);
    }
}
