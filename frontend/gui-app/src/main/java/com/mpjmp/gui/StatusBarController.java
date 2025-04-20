package com.mpjmp.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.util.Timer;
import java.util.TimerTask;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.application.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import com.dataorchestrate.common.DeviceConfigUtil;

public class StatusBarController {
    @FXML private Label statusLabel;
    
    private Timer healthTimer;
    
    public void initialize() {
        healthTimer = new Timer();
        healthTimer.schedule(new TimerTask() {
            public void run() {
                updateHealthStatus();
            }
        }, 0, 30000); // Check every 30 seconds
    }
    
    private void updateHealthStatus() {
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                    .uri(URI.create(getHealthStatusUrl()))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            
            Platform.runLater(() -> {
                if (response.statusCode() == 200) {
                    statusLabel.setText("Status: Healthy");
                    statusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    statusLabel.setText("Status: Degraded");
                    statusLabel.setStyle("-fx-text-fill: orange;");
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                statusLabel.setText("Status: Offline");
                statusLabel.setStyle("-fx-text-fill: red;");
            });
        }
    }
    
    // Use device info from devices.json for health status URL
    private String getHealthStatusUrl() {
        try {
            List<Map<String, String>> allDevices = DeviceConfigUtil.getAllDevices();
            String selfDeviceName = DeviceConfigUtil.getSelfDeviceName();
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(selfDeviceName)).findFirst().orElse(null);
            if (self != null) {
                return "http://" + self.get("ip") + ":" + self.get("notification_port") + "/health";
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load device config", e);
        }
        throw new RuntimeException("Self device not found in config");
    }
}
