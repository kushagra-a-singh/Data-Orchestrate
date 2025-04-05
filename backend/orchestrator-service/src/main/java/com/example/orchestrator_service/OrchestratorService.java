package com.example.orchestrator_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.orchestrator_service.model.DeviceInfo;
import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Service
public class OrchestratorService {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);
    
    @Autowired
    private MongoTemplate mongoTemplate;

    @KafkaListener(topics = "file-upload-topic", groupId = "orchestrator-group")
    public void handleFileUpload(String fileId, String userId, String userIp) {
        try {
            // Get source device IP from MongoDB
            String sourceDeviceIp = getSourceDeviceIp(userIp);
            
            // Get list of active devices from MongoDB
            List<DeviceInfo> activeDevices = getActiveDevices();
            
            // Replicate file to all active devices except source
            replicateFileToDevices(fileId, sourceDeviceIp, activeDevices);
            
            logger.info("File {} replicated successfully from {} to {} devices", 
                fileId, sourceDeviceIp, activeDevices.size() - 1);
        } catch (Exception e) {
            logger.error("Error processing file upload: {}", e.getMessage());
        }
    }

    private void replicateFileToDevices(String fileId, String sourceDeviceIp, List<DeviceInfo> devices) {
        try {
            for (DeviceInfo device : devices) {
                if (!device.getIpAddress().equals(sourceDeviceIp)) {
                    restTemplate.postForObject(
                        "http://" + device.getIpAddress() + "/replicate/" + fileId,
                        null,
                        String.class
                    );
                    logger.info("Replicated file {} to device {}", fileId, device.getIpAddress());
                }
            }
        } catch (Exception e) {
            logger.error("Error replicating file to devices: {}", e.getMessage());
        }
    }

    private String getSourceDeviceIp(String userIp) {
        // Find the device that matches the user's IP
        DeviceInfo sourceDevice = mongoTemplate.findOne(
            Query.query(Criteria.where("ipAddress").regex(userIp + ".*")),
            DeviceInfo.class
        );
        return sourceDevice != null ? sourceDevice.getIpAddress() : userIp;
    }

    private List<DeviceInfo> getActiveDevices() {
        // Get all devices that were active in the last 5 minutes
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        return mongoTemplate.find(
            Query.query(Criteria.where("lastActive").gt(fiveMinutesAgo)),
            DeviceInfo.class
        );
    }
}

