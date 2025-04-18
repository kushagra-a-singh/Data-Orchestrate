package com.mpjmp.gui.util;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.Properties;

public class DeviceIdentifier {
    private static String deviceId;
    private static String deviceName;

    static {
        try {
            // Load from config file if exists
            Properties props = new Properties();
            File config = new File(System.getProperty("user.home"), ".mpjmp-device.properties");
            if (config.exists()) {
                try (FileInputStream in = new FileInputStream(config)) {
                    props.load(in);
                    deviceId = props.getProperty("deviceId");
                    deviceName = props.getProperty("deviceName");
                }
            }
            // Fallback: Use hostname
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = InetAddress.getLocalHost().getHostName();
            }
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = deviceId;
            }
        } catch (Exception e) {
            deviceId = "UNKNOWN";
            deviceName = "UNKNOWN";
        }
    }

    public static String getDeviceId() {
        return deviceId;
    }

    public static String getDeviceName() {
        return deviceName;
    }
}
