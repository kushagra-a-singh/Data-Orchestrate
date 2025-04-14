package com.dataorchestrate.common;

import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DeviceIdentifier {
    private static final Logger logger = LoggerFactory.getLogger(DeviceIdentifier.class);
    private String deviceId;
    
    public DeviceIdentifier() {
        this.deviceId = generateDeviceId();
        logger.info("Generated Device ID: {}", deviceId);
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    private String generateDeviceId() {
        try {
            // Get hostname
            String hostname = InetAddress.getLocalHost().getHostName();
            logger.debug("Hostname: {}", hostname);
            
            // Get MAC address
            NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            byte[] mac = network.getHardwareAddress();
            
            // Create a unique ID from hostname and MAC
            String macString = String.format("%02X%02X%02X%02X%02X%02X", 
                mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
            logger.debug("MAC Address: {}", macString);
            
            // Combine hostname and MAC, but limit length and remove special characters
            String baseId = (hostname + "-" + macString)
                .replaceAll("[^a-zA-Z0-9-]", "")
                .substring(0, Math.min(32, (hostname + "-" + macString).length()));
            
            return "DEVICE-" + baseId;
        } catch (Exception e) {
            logger.warn("Failed to generate device ID from host info, falling back to UUID", e);
            // Fallback to a random UUID if we can't get host info
            return "DEVICE-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
} 