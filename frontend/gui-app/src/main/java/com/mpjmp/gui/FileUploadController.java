package com.mpjmp.gui;

import com.mpjmp.gui.api.FileUploadService;
import com.mpjmp.gui.api.ProgressTrackingService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;

import java.io.File;

public class FileUploadController {
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    private File selectedFile;

    @FXML
    public void chooseFile() {
        FileChooser fileChooser = new FileChooser();
        selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            statusLabel.setText("Selected: " + selectedFile.getName());
        }
    }

    @FXML
    public void uploadFile() {
        if (selectedFile == null) {
            statusLabel.setText("No file selected!");
            return;
        }

        new Thread(() -> {
            String response = FileUploadService.uploadFile(selectedFile);
            Platform.runLater(() -> statusLabel.setText(response));

            // Start tracking progress
            while (true) {
                String progress = ProgressTrackingService.getProcessingProgress(selectedFile.getName());
                Platform.runLater(() -> progressBar.setProgress(Double.parseDouble(progress)));

                if (progress.equals("1.0")) {
                    Platform.runLater(() -> statusLabel.setText("Processing Complete!"));
                    break;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }
}
