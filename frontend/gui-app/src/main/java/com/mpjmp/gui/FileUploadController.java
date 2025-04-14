package com.mpjmp.gui;

import com.mpjmp.gui.api.FileUploadService;
import com.mpjmp.gui.api.ProgressTrackingService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.File;

public class FileUploadController {
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button uploadButton;

    private File selectedFile;

    @FXML
    public void initialize() {
        uploadButton.setDisable(true);
        updateStatus("Select a file to begin", Color.GRAY);
    }

    @FXML
    public void chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        selectedFile = fileChooser.showOpenDialog(null);
        
        if (selectedFile != null) {
            updateStatus("Selected: " + selectedFile.getName(), Color.GREEN);
            uploadButton.setDisable(false);
        } else {
            updateStatus("No file selected", Color.GRAY);
            uploadButton.setDisable(true);
        }
    }

    @FXML
    public void uploadFile() {
        if (selectedFile == null) {
            updateStatus("No file selected!", Color.RED);
            return;
        }

        uploadButton.setDisable(true);
        updateStatus("Uploading...", Color.BLUE);
        progressBar.setProgress(-1); // Indeterminate progress

        new Thread(() -> {
            try {
                String response = FileUploadService.uploadFile(selectedFile);
                
                if (response.contains("successful")) {
                    Platform.runLater(() -> {
                        updateStatus("Upload successful! Processing...", Color.GREEN);
                        progressBar.setProgress(0);
                    });

                    // Start tracking progress
                    int attempts = 0;
                    int maxAttempts = 10; // Limit the number of attempts
                    
                    while (attempts < maxAttempts) {
                        String progress = ProgressTrackingService.getProcessingProgress(selectedFile.getName());
                        
                        try {
                            double progressValue = Double.parseDouble(progress);
                            
                            Platform.runLater(() -> {
                                progressBar.setProgress(progressValue);
                                if (progressValue < 1.0) {
                                    updateStatus("Processing: " + (int)(progressValue * 100) + "%", Color.BLUE);
                                } else {
                                    updateStatus("Processing Complete!", Color.GREEN);
                                    uploadButton.setDisable(false);
                                }
                            });

                            if (progressValue >= 1.0) {
                                break;
                            }
                        } catch (NumberFormatException e) {
                            // If we can't parse the progress value, just continue
                            System.err.println("Error parsing progress value: " + progress);
                        }
                        
                        attempts++;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}
                    }
                    
                    // If we've reached the maximum number of attempts, just show completion
                    if (attempts >= maxAttempts) {
                        Platform.runLater(() -> {
                            updateStatus("Processing Complete!", Color.GREEN);
                            uploadButton.setDisable(false);
                            progressBar.setProgress(1.0);
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        updateStatus("Upload failed: " + response, Color.RED);
                        uploadButton.setDisable(false);
                        progressBar.setProgress(0);
                        
                        // Show error dialog for connection issues
                        if (response.contains("File upload service is not running")) {
                            showErrorDialog("Service Not Running", 
                                "The file upload service is not running. Please start the service first.");
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Error: " + e.getMessage(), Color.RED);
                    uploadButton.setDisable(false);
                    progressBar.setProgress(0);
                });
            }
        }).start();
    }

    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }
    
    private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
