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
                    .uri(URI.create("http://localhost:8080/health"))
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
}
