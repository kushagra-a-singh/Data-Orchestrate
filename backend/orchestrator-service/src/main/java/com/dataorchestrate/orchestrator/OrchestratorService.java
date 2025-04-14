package com.dataorchestrate.orchestrator;

import com.dataorchestrate.common.DeviceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrchestratorService {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DeviceIdentifier deviceIdentifier;
    
    @Autowired
    public OrchestratorService(
            KafkaTemplate<String, String> kafkaTemplate,
            DeviceIdentifier deviceIdentifier) {
        this.kafkaTemplate = kafkaTemplate;
        this.deviceIdentifier = deviceIdentifier;
        logger.info("Initializing OrchestratorService for device: {}", deviceIdentifier.getDeviceId());
    }
    
    public void handleFileEvent(String fileId, String fileName, String eventType) {
        String deviceId = deviceIdentifier.getDeviceId();
        logger.info("Device {} handling {} event for file {}", deviceId, eventType, fileName);
        
        switch (eventType) {
            case "UPLOAD":
                kafkaTemplate.send("file.uploaded", 
                    String.format("File %s uploaded by device %s", fileName, deviceId));
                break;
            case "PROCESS":
                kafkaTemplate.send("file.processing", 
                    String.format("File %s sent for processing by device %s", fileName, deviceId));
                break;
            case "SYNC":
                kafkaTemplate.send("file.synced", 
                    String.format("File %s synced by device %s", fileName, deviceId));
                break;
            default:
                logger.warn("Unknown event type: {}", eventType);
        }
    }
} 