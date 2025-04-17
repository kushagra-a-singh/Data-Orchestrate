package com.dataorchestrate.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service to clean up invalid device entries from the database
 */
@Service
public class DeviceCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceCleanupService.class);
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    /**
     * Cleans up invalid device entries on application startup
     * This will run once when the application starts
     */
    @PostConstruct
    public void cleanupInvalidDevices() {
        try {
            logger.info("Starting cleanup of invalid device entries...");
            
            // Create a query to find devices with "UNKNOWN" in the device name
            Query query = new Query(Criteria.where("deviceName").regex("UNKNOWN"));
            
            // Delete the matching documents
            long deletedCount = mongoTemplate.remove(query, Device.class).getDeletedCount();
            
            logger.info("Cleaned up {} invalid device entries", deletedCount);
        } catch (Exception e) {
            logger.error("Error during device cleanup", e);
        }
    }
    
    /**
     * Manual method to reset the device sequence
     * Call this method if you want to reset the device numbering
     */
    public void resetDeviceSequence() {
        try {
            // Delete the sequence document
            mongoTemplate.remove(
                Query.query(Criteria.where("_id").is("device_sequence")), 
                "database_sequences");
            
            logger.info("Device sequence has been reset");
        } catch (Exception e) {
            logger.error("Error resetting device sequence", e);
        }
    }
}
