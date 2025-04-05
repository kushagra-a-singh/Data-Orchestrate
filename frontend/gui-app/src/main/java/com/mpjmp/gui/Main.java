package com.mpjmp.gui;

import com.mpjmp.gui.service.NotificationService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {
    private NotificationService notificationService;

    @Override
    public void start(Stage stage) throws IOException {
        // Initialize notification service
        notificationService = new NotificationService();
        notificationService.startPolling();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Distributed File Processor");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Clean up resources
        if (notificationService != null) {
            notificationService.stopPolling();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
