package com.mpjmp.gui.utils;

import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataorchestrate.common.DeviceConfigUtil;

public class NotificationWebSocketClient extends WebSocketClient {
    private static NotificationWebSocketClient instance;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public interface NotificationHandler {
        void onNotification(Map<String, Object> notification);
    }
    private NotificationHandler handler;

    // Dynamically construct WebSocket URL from devices.json
    private static String getWebSocketUrl() {
        try {
            List<Map<String, String>> allDevices = DeviceConfigUtil.getAllDevices();
            String selfDeviceName = DeviceConfigUtil.getSelfDeviceName();
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(selfDeviceName)).findFirst().orElse(null);
            if (self != null) {
                return "ws://" + self.get("ip") + ":" + self.get("notification_port") + "/ws";
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load device config for WebSocket", e);
        }
        throw new RuntimeException("Self device not found in config for WebSocket");
    }

    public NotificationWebSocketClient(NotificationHandler handler) throws Exception {
        super(new URI(getWebSocketUrl()));
        this.handler = handler;
    }

    public static void start(NotificationHandler handler) {
        if (instance == null) {
            try {
                instance = new NotificationWebSocketClient(handler);
                instance.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("WebSocket connected to notification service");
    }

    @Override
    public void onMessage(String message) {
        try {
            Map<String, Object> notification = objectMapper.readValue(message, Map.class);
            if (handler != null) {
                Platform.runLater(() -> handler.onNotification(notification));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("WebSocket closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
    }
}
