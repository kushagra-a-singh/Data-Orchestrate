package com.mpjmp.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import io.github.cdimascio.dotenv.Dotenv;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Get the WebSocket client instance but don't connect yet
        // This allows the application to start even if the notification service is down
        WebSocketClient.getInstance();
        
        // Load the main FXML view
        Parent root = FXMLLoader.load(getClass().getResource("/views/main.fxml"));
        primaryStage.setTitle("MPJ-MP Data Orchestrator");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Use absolute path for .env file location
        String envPath = "D:/Kushagra/Programming/MPJ-MP/Data-Orchestrate";
        System.out.println("[DEBUG] Dotenv will search for .env in: " + envPath);
        Dotenv dotenv = Dotenv.configure()
            .directory(envPath)
            .ignoreIfMissing()
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
