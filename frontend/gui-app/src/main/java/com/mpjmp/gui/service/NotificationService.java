package com.mpjmp.gui.service;

import com.mpjmp.gui.NotificationWebSocketClient;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationService {
    private final NotificationWebSocketClient webSocketClient;
    private volatile boolean isRunning;

    public NotificationService() {
        this.webSocketClient = new NotificationWebSocketClient(this);
        this.isRunning = false;
    }

    public void startPolling() {
        if (!isRunning) {
            isRunning = true;
            webSocketClient.connect();
            log.info("Notification service started");
        }
    }

    public void stopPolling() {
        if (isRunning) {
            isRunning = false;
            webSocketClient.disconnect();
            log.info("Notification service stopped");
        }
    }

    public void handleNotification(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Notification");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
} 