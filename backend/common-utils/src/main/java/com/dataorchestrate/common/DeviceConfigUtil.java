package com.dataorchestrate.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DeviceConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(DeviceConfigUtil.class);
    private static List<Map<String, String>> devices = new ArrayList<>();
    private static Map<String, String> selfDevice = null;
    private static String selfDeviceName = null;

    static {
        try {
            // Try to load from classpath first
            InputStream is = DeviceConfigUtil.class.getClassLoader().getResourceAsStream("devices.json");
            if (is == null) {
                // Fallback to file in resources
                File file = new File("backend/common-utils/src/main/resources/devices.json");
                is = file.exists() ? new java.io.FileInputStream(file) : null;
            }
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper();
                devices = mapper.readValue(is, new TypeReference<List<Map<String, String>>>() {});
                is.close();
                // Prefer env variable
                selfDeviceName = System.getenv("DEVICE_NAME");
                if (selfDeviceName == null || selfDeviceName.isEmpty()) {
                    // Try system property (e.g. -DDEVICE_NAME=...)
                    selfDeviceName = System.getProperty("DEVICE_NAME");
                }
                if (selfDeviceName == null || selfDeviceName.isEmpty()) {
                    // Try Windows username as fallback
                    String winUser = System.getenv("USERNAME");
                    if (winUser != null && !winUser.isEmpty()) {
                        Optional<Map<String, String>> match = devices.stream().filter(d -> winUser.equalsIgnoreCase(d.get("name")) || winUser.equalsIgnoreCase(d.get("hostname"))).findFirst();
                        if (match.isPresent()) {
                            selfDeviceName = match.get().get("name");
                        }
                    }
                }
                if (selfDeviceName == null || selfDeviceName.isEmpty()) {
                    // Fallback: try to auto-detect by hostname
                    String hostname = java.net.InetAddress.getLocalHost().getHostName();
                    Optional<Map<String, String>> match = devices.stream().filter(d -> hostname.equalsIgnoreCase(d.get("name")) || hostname.equalsIgnoreCase(d.get("hostname"))).findFirst();
                    if (match.isPresent()) {
                        selfDeviceName = match.get().get("name");
                    }
                }
                if (selfDeviceName != null && !selfDeviceName.isEmpty()) {
                    selfDevice = devices.stream().filter(d -> selfDeviceName.equals(d.get("name"))).findFirst().orElse(null);
                }
                if (selfDevice == null) {
                    logger.warn("Could not identify self device. Set DEVICE_NAME env variable or ensure hostname matches devices.json");
                }
            } else {
                logger.error("devices.json not found!");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize DeviceConfigUtil", e);
        }
    }

    public static Map<String, String> getSelfDevice() {
        return selfDevice;
    }

    public static String getSelfDeviceName() {
        return selfDevice != null ? selfDevice.get("name") : null;
    }

    public static List<Map<String, String>> getAllDevices() {
        return devices;
    }

    public static List<Map<String, String>> getPeerDevices() {
        if (selfDevice == null) return devices;
        return devices.stream().filter(d -> !Objects.equals(d.get("name"), selfDevice.get("name"))).collect(Collectors.toList());
    }

    public static Map<String, String> getDeviceByName(String name) {
        return devices.stream().filter(dev -> name.equals(dev.get("name"))).findFirst().orElse(null);
    }

    public static Map<String, String> getDeviceByIp(String ip) {
        return devices.stream().filter(dev -> ip.equals(dev.get("ip"))).findFirst().orElse(null);
    }
}
