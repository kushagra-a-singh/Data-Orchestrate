package com.dataorchestrate.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;

@Component
public class DeviceIdentifier {
    private static final Logger logger = LoggerFactory.getLogger(DeviceIdentifier.class);
    private static final String DEVICE_SEQUENCE_NAME = "device_sequence";
    
    private String deviceId;
    private String deviceName;
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    @Autowired
    private SequenceGenerator sequenceGenerator;
    
    public DeviceIdentifier() {
        // Initialization will be done in the init method
    }
    
    /**
     * Initialize the device identifier after dependency injection
     */
    @PostConstruct
    public void init() {
        try {
            generateDeviceInfo();
            logger.info("Generated Device ID: {}, Device Name: {}", deviceId, deviceName);
        } catch (Exception e) {
            logger.error("Error initializing DeviceIdentifier", e);
        }
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    private void generateDeviceInfo() {
        try {
            // Get hostname
            String hostname = InetAddress.getLocalHost().getHostName();
            logger.debug("Hostname: {}", hostname);
            
            // Get MAC address
            NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            byte[] mac = network.getHardwareAddress();
            
            // Create a MAC address string
            String macString = String.format("%02X%02X%02X%02X%02X%02X", 
                mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
            logger.debug("MAC Address: {}", macString);
            
            // Check if this device already exists in the database
            Optional<Device> existingDevice = deviceRepository.findByMacAddress(macString);
            
            if (existingDevice.isPresent()) {
                // Device already exists, use its information
                Device device = existingDevice.get();
                this.deviceId = device.getDeviceId();
                this.deviceName = device.getDeviceName();
                
                // Update last seen timestamp
                device.setLastSeen(java.time.LocalDateTime.now());
                device.setStatus("ONLINE");
                deviceRepository.save(device);
                
                logger.info("Found existing device: {}", device);
            } else {
                // Create a new device with a sequential ID
                long sequence = sequenceGenerator.generateSequence(DEVICE_SEQUENCE_NAME);
                
                // Set device name to hostname
                this.deviceName = hostname;
                
                // Generate device ID with sequence number
                this.deviceId = "DEVICE-" + sequence;
                
                // Save the device to the database
                Device newDevice = new Device();
                newDevice.setDeviceId(this.deviceId);
                newDevice.setDeviceName(this.deviceName);
                newDevice.setMacAddress(macString);
                newDevice.setIpAddress(InetAddress.getLocalHost().getHostAddress());
                
                deviceRepository.save(newDevice);
                logger.info("Created new device: {}", newDevice);
            }
        } catch (Exception e) {
            logger.warn("Failed to generate device info from host info, falling back to UUID", e);
            // Fallback to a random UUID if we can't get host info
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            
            try {
                // Still try to generate a sequential ID
                if (sequenceGenerator != null) {
                    long sequence = sequenceGenerator.generateSequence(DEVICE_SEQUENCE_NAME);
                    this.deviceId = "DEVICE-" + sequence;
                } else {
                    this.deviceId = "DEVICE-" + uuid;
                }
                
                this.deviceName = "UNKNOWN-" + uuid;
                
                // Save the device to the database if repository is available
                if (deviceRepository != null) {
                    Device newDevice = new Device();
                    newDevice.setDeviceId(this.deviceId);
                    newDevice.setDeviceName(this.deviceName);
                    newDevice.setIpAddress(InetAddress.getLocalHost().getHostAddress());
                    
                    deviceRepository.save(newDevice);
                    logger.info("Created fallback device: {}", newDevice);
                }
            } catch (Exception ex) {
                // Final fallback if everything fails
                this.deviceId = "DEVICE-" + uuid;
                this.deviceName = "UNKNOWN-" + uuid;
                logger.error("Failed to save device to database", ex);
            }
        }
    }
}