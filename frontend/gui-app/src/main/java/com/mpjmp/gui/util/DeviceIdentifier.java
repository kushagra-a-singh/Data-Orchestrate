package com.mpjmp.gui.util;

import com.dataorchestrate.common.DeviceConfigUtil;

public class DeviceIdentifier {
    private static String deviceId;
    private static String deviceName;

    static {
        try {
            // Use backend DeviceConfigUtil for device info
            deviceId = DeviceConfigUtil.getSelfDeviceName();
            deviceName = deviceId;
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
