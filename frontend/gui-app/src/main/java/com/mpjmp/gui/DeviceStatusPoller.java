package com.mpjmp.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeviceStatusPoller {
    private static final String DEVICE_API_URL = "http://localhost:8081/api/devices";
    private static final String HEALTH_TEMPLATE = "http://%s:8081/api/health";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ObjectMapper objectMapper = new ObjectMapper();
    public void start() {
        scheduler.scheduleAtFixedRate(this::pollDevices, 0, 30, TimeUnit.SECONDS);
    }
    private void pollDevices() {
        try {
            URL url = new URL(DEVICE_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            InputStream is = conn.getInputStream();
            List<Map<String, Object>> rawDevices = objectMapper.readValue(is, new TypeReference<List<Map<String, Object>>>(){});
            List<DeviceStatus> deviceStatuses = new ArrayList<>();
            for (Map<String, Object> map : rawDevices) {
                // Map to DeviceStatus
                String deviceName = (String) map.getOrDefault("deviceName", "?");
                String deviceId = (String) map.getOrDefault("deviceId", "?");
                String status = (String) map.getOrDefault("status", "OFFLINE");
                String ip = (String) map.getOrDefault("ip", "127.0.0.1");
                String reachable = testHealth(ip) ? "Yes" : "No";
                deviceStatuses.add(new DeviceStatus(deviceName, deviceId, status, reachable));
            }
            Platform.runLater(() -> DeviceStatusPanel.getInstance().updateDevices(deviceStatuses));
        } catch (Exception e) {
            System.err.println("Device polling failed: " + e.getMessage());
        }
    }
    private boolean testHealth(String ip) {
        try {
            String urlStr = String.format(HEALTH_TEMPLATE, ip);
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
