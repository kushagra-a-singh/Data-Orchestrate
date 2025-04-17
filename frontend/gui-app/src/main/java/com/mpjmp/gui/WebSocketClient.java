package com.mpjmp.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javax.websocket.*;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ClientEndpoint
public class WebSocketClient {
    private static WebSocketClient instance;
    private Session session;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean isConnected = false;
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();

    private WebSocketClient() {
        // Connect after a short delay to ensure the application has started
        Platform.runLater(() -> {
            try {
                Thread.sleep(1000); // Small delay to ensure UI is ready
                connect();
            } catch (Exception e) {
                System.err.println("Error during initial connection: " + e.getMessage());
            }
        });
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
            System.out.println("Attempting to connect to WebSocket...");
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.setDefaultMaxSessionIdleTimeout(0); // No timeout
            
            // Use SockJS compatible endpoint
            container.connectToServer(this, new URI("ws://localhost:8081/ws/websocket"));
        } catch (Exception e) {
            System.err.println("WebSocket Connection Failed: " + e.getMessage());
            // Schedule reconnection attempt
            reconnectExecutor.schedule(this::connect, 5, TimeUnit.SECONDS);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.isConnected = true;
        System.out.println("Connected to notification service");
        try {
            // STOMP handshake for notifications, logs, and replication-status
            session.getBasicRemote().sendText("CONNECT\naccept-version:1.1,1.0\nheart-beat:10000,10000\n\n\0");
            
            // Add small delay between subscriptions to avoid overwhelming the server
            Thread.sleep(100);
            session.getBasicRemote().sendText("SUBSCRIBE\nid:sub-0\ndestination:/topic/notifications\n\n\0");
            
            Thread.sleep(100);
            session.getBasicRemote().sendText("SUBSCRIBE\nid:sub-1\ndestination:/topic/logs\n\n\0");
            
            Thread.sleep(100);
            session.getBasicRemote().sendText("SUBSCRIBE\nid:sub-2\ndestination:/topic/replication-status\n\n\0");
            
            Thread.sleep(100);
            session.getBasicRemote().sendText("SUBSCRIBE\nid:sub-3\ndestination:/topic/file-events\n\n\0");
            
            System.out.println("Successfully subscribed to all topics");
        } catch (Exception e) {
            System.err.println("Failed to subscribe to topics: " + e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            System.out.println("Received WebSocket message: " + message.substring(0, Math.min(50, message.length())) + "...");
            
            // Handle STOMP protocol messages
            if (message.startsWith("CONNECTED")) {
                // This is a STOMP connection acknowledgment - no need to process
                System.out.println("STOMP connection established");
                return;
            }
            
            // Check for STOMP message frame
            if (message.startsWith("MESSAGE")) {
                // Extract the body from the STOMP frame
                int bodyIndex = message.indexOf("\n\n");
                if (bodyIndex > 0) {
                    String body = message.substring(bodyIndex + 2).replace("\0", "");
                    
                    // Only try to parse as JSON if it looks like JSON
                    if (body.trim().startsWith("{")) {
                        processMessageBody(body, message);
                    } else {
                        // Handle non-JSON message body
                        System.out.println("Received non-JSON message: " + body);
                        
                        // Still try to handle it as a log message
                        Platform.runLater(() -> {
                            try {
                                LogPanel.getInstance().appendLog(body);
                            } catch (Exception e) {
                                System.err.println("Error appending log: " + e.getMessage());
                            }
                        });
                    }
                }
                return;
            }
            
            // Handle other non-STOMP messages
            if (message.trim().startsWith("{")) {
                // Looks like JSON, try to parse it
                processMessageBody(message, message);
            } else {
                // Non-JSON message, just log it
                System.out.println("Received non-JSON message: " + message);
            }
        } catch (Exception e) {
            System.err.println("Error processing WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void processMessageBody(String body, String originalMessage) {
        try {
            Map<String, Object> data = objectMapper.readValue(body, 
                new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
            
            if (originalMessage.contains("replication-status") || data.containsKey("status")) {
                // Handle replication status event
                String fileName = (String) data.get("fileName");
                String status = (String) data.get("status");
                String targetDevice = (String) data.get("targetDeviceId");
                
                if (fileName != null && status != null) {
                    Platform.runLater(() -> {
                        try {
                            ReplicationStatusPanel.getInstance().updateStatus(fileName, status, targetDevice);
                        } catch (Exception e) {
                            System.err.println("Error updating replication status: " + e.getMessage());
                        }
                    });
                }
                return;
            }
            
            if (originalMessage.contains("logs") || data.containsKey("logMessage")) {
                // Handle log event
                String logMessage = (String) data.get("logMessage");
                if (logMessage == null) logMessage = body; // Use whole body if no specific message
                
                final String finalLogMessage = logMessage;
                Platform.runLater(() -> {
                    try {
                        LogPanel.getInstance().appendLog(finalLogMessage);
                    } catch (Exception e) {
                        System.err.println("Error appending log: " + e.getMessage());
                    }
                });
                return;
            }
            
            // Default: treat as notification
            String type = (String) data.get("type");
            String notificationMessage = (String) data.get("message");
            
            if (notificationMessage != null) {
                Platform.runLater(() -> {
                    try {
                        Alert alert = new Alert(type != null && type.equals("SUCCESS") ? AlertType.INFORMATION : AlertType.ERROR);
                        alert.setTitle("File Processing Notification");
                        alert.setHeaderText(null);
                        alert.setContentText(notificationMessage);
                        alert.showAndWait();
                    } catch (Exception e) {
                        System.err.println("Error showing notification: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error parsing message body: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Disconnected from notification service");
        this.session = null;
        this.isConnected = false;
        
        // Schedule reconnection attempt
        reconnectExecutor.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket Error: " + throwable.getMessage());
        this.isConnected = false;
        
        // Schedule reconnection attempt
        reconnectExecutor.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    private void showError(String title, String content) {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            } catch (Exception e) {
                System.err.println("Error showing error dialog: " + e.getMessage());
            }
        });
    }
}
