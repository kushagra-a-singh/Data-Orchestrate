package com.mpjmp.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javax.websocket.*;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;

@ClientEndpoint
public class WebSocketClient {
    private static WebSocketClient instance;
    private Session session;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean isConnected = false;

    private WebSocketClient() {
        // Don't try to connect immediately - let the application start first
        // We'll connect when needed
    }

    public static WebSocketClient getInstance() {
        if (instance == null) {
            instance = new WebSocketClient();
        }
        return instance;
    }
    
    public void connect() {
        if (isConnected) return;
        
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            // Connect to the notification service WebSocket endpoint with SockJS
            container.connectToServer(this, new URI("ws://localhost:8084/ws/websocket"));
            isConnected = true;
        } catch (Exception e) {
            System.err.println("WebSocket Connection Failed: " + e.getMessage());
            // Don't show error dialog - just log it
            // This allows the application to start even if the notification service is down
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("Connected to notification service");
        
        // Subscribe to notifications topic
        try {
            session.getBasicRemote().sendText("CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\0");
            session.getBasicRemote().sendText("SUBSCRIBE\nid:sub-0\ndestination:/topic/notifications\n\n\0");
        } catch (Exception e) {
            System.err.println("Failed to subscribe to notifications: " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            // Parse the notification message
            Map<String, Object> notification = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
            String type = (String) notification.get("type");
            String notificationMessage = (String) notification.get("message");
            
            // Show notification in the UI thread
            Platform.runLater(() -> {
                Alert alert = new Alert(type.equals("SUCCESS") ? AlertType.INFORMATION : AlertType.ERROR);
                alert.setTitle("File Processing Notification");
                alert.setHeaderText(null);
                alert.setContentText(notificationMessage);
                alert.showAndWait();
            });
        } catch (Exception e) {
            System.err.println("Error processing notification: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Disconnected from notification service");
        this.session = null;
        isConnected = false;
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket Error: " + throwable.getMessage());
        // Don't show error dialog - just log it
    }

    private void showError(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
