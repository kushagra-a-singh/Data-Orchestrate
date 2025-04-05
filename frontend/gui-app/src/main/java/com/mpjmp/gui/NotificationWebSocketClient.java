package com.mpjmp.gui;

import com.mpjmp.gui.service.NotificationService;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class NotificationWebSocketClient extends TextWebSocketHandler {
    private static final String NOTIFICATION_SERVICE_URL = "ws://localhost:8084/ws/notifications";
    private WebSocketClient webSocketClient;
    private WebSocketSession session;
    private final NotificationService notificationService;

    public NotificationWebSocketClient(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void connect() {
        webSocketClient = new StandardWebSocketClient();
        CompletableFuture.runAsync(() -> {
            try {
                session = webSocketClient.execute(this, NOTIFICATION_SERVICE_URL).get();
                log.info("Connected to notification service");
            } catch (Exception e) {
                log.error("Failed to connect to notification service", e);
            }
        });
    }

    public void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("Disconnected from notification service");
            } catch (Exception e) {
                log.error("Error disconnecting from notification service", e);
            }
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("Received notification: {}", payload);
        
        Platform.runLater(() -> {
            notificationService.handleNotification(payload);
        });
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        log.info("WebSocket connection closed: {}", status);
    }
}
