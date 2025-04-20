package com.mpjmp.gui.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.dataorchestrate.common.DeviceConfigUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

public class BackendDeviceIdProvider {
    private static String backendDeviceId = null;
    private static List<Map<String, String>> allDevices;
    private static String selfDeviceName;
    private static String deviceIdUrl;

    static {
        try {
            allDevices = DeviceConfigUtil.getAllDevices();
            selfDeviceName = DeviceConfigUtil.getSelfDeviceName();
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(selfDeviceName)).findFirst().orElse(null);
            if (self != null) {
                deviceIdUrl = "http://" + self.get("ip") + ":" + self.get("port") + "/api/files/device/id";
            } else {
                deviceIdUrl = null;
            }
        } catch (Exception e) {
            selfDeviceName = "UNKNOWN";
            deviceIdUrl = null;
        }
    }

    public static String getBackendDeviceId() {
        if (backendDeviceId != null) return backendDeviceId;
        try {
            URL url = new URL(deviceIdUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int status = conn.getResponseCode();
            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
                in.close();
                JSONObject obj = new JSONObject(content.toString());
                backendDeviceId = obj.optString("deviceId", null);
                if (backendDeviceId == null || backendDeviceId.isEmpty()) {
                    backendDeviceId = null;
                }
            }
        } catch (Exception e) {
            // Could not fetch from backend
            backendDeviceId = null;
        }
        return backendDeviceId;
    }
}
