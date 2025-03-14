package com.mpjmp.gui;

import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
public class WebSocketClient {
    private static WebSocketClient instance;
    private Session session;
    private WebSocketListener listener;

    public interface WebSocketListener {
        void onMessageReceived(String message);
    }

    private WebSocketClient() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI("ws://localhost:8083/progress-updates"));
        } catch (Exception e) {
            System.out.println("WebSocket Connection Failed: " + e.getMessage());
        }
    }

    public static WebSocketClient getInstance() {
        if (instance == null) {
            instance = new WebSocketClient();
        }
        return instance;
    }

    public void setListener(WebSocketListener listener) {
        this.listener = listener;
    }

    @OnMessage
    public void onMessage(String message) {
        if (listener != null) {
            listener.onMessageReceived(message);
        }
    }
}
