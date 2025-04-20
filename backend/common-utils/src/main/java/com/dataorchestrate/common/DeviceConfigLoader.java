package com.dataorchestrate.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DeviceConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(DeviceConfigLoader.class);
    private List<Map<String, String>> devices;
    private Map<String, String> self;

    public DeviceConfigLoader(String configPath, String deviceName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream(configPath.replace("classpath:", ""));
            devices = mapper.readValue(is, new TypeReference<List<Map<String, String>>>() {});
            if (deviceName != null && !deviceName.isEmpty()) {
                Optional<Map<String, String>> found = devices.stream().filter(d -> deviceName.equals(d.get("name"))).findFirst();
                if (found.isPresent()) {
                    self = found.get();
                } else {
                    throw new RuntimeException("Device name not found in config: " + deviceName);
                }
            } else {
                String localIp = InetAddress.getLocalHost().getHostAddress();
                Optional<Map<String, String>> found = devices.stream().filter(d -> localIp.equals(d.get("ip"))).findFirst();
                if (found.isPresent()) {
                    self = found.get();
                } else {
                    throw new RuntimeException("Local IP not found in config: " + localIp);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load device config", e);
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getSelf() {
        return self;
    }

    public List<Map<String, String>> getPeers() {
        List<Map<String, String>> peers = new ArrayList<>();
        for (Map<String, String> d : devices) {
            if (!d.get("name").equals(self.get("name"))) {
                peers.add(d);
            }
        }
        return peers;
    }

    public List<Map<String, String>> getAllDevices() {
        return devices;
    }
}
