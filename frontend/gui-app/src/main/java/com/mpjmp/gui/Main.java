package com.mpjmp.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import io.github.cdimascio.dotenv.Dotenv;
import com.mpjmp.gui.utils.NotificationWebSocketClient;
import com.mpjmp.gui.utils.NotificationUtil;
import java.nio.file.*;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Start WebSocket client for notifications
        NotificationWebSocketClient.start(notification -> {
            String type = (String) notification.getOrDefault("type", "info");
            String title = (String) notification.getOrDefault("title", "Notification");
            String message = (String) notification.getOrDefault("message", "");
            switch (type) {
                case "success":
                    NotificationUtil.showSuccess(title, message);
                    break;
                case "error":
                    NotificationUtil.showError(title, message);
                    break;
                default:
                    NotificationUtil.showInfo(title, message);
                    break;
            }
        });
        
        // Load the main FXML view
        Parent root = FXMLLoader.load(getClass().getResource("/views/main.fxml"));
        primaryStage.setTitle("MPJ-MP Data Orchestrator");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    private static String findEnvDirectory() {
        Path dir = Paths.get(System.getProperty("user.dir"));
        while (dir != null) {
            if (Files.exists(dir.resolve(".env"))) {
                return dir.toString();
            }
            dir = dir.getParent();
        }
        // Fallback: use current working directory if not found
        return System.getProperty("user.dir");
    }

    public static void main(String[] args) {
        // Use directory search for .env file location
        String envDir = findEnvDirectory();
        System.out.println("[DEBUG] Dotenv will search for .env in: " + envDir);
        Dotenv dotenv = Dotenv.configure()
            .directory(envDir)
            .load();
        
        // Print MongoDB connection info
        String mongoDbUri = dotenv.get("MONGODB_URI");
        String mongoDbDatabase = dotenv.get("MONGODB_DATABASE");
        System.out.println("MongoDB URI: " + mongoDbUri);
        System.out.println("MongoDB Database: " + mongoDbDatabase);
        
        // Make environment variables available to the application using System Properties
        // This is necessary because System.getenv() can't be modified at runtime
        if (mongoDbUri != null) {
            System.setProperty("MONGODB_URI", mongoDbUri);
        }
        if (mongoDbDatabase != null) {
            System.setProperty("MONGODB_DATABASE", mongoDbDatabase);
        }
        
        // Set other important environment variables from .env
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        
        launch(args);
    }
}
