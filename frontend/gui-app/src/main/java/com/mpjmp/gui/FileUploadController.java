package com.mpjmp.gui;

import com.mpjmp.gui.api.FileUploadService;
import com.mpjmp.gui.api.ProgressTrackingService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.stage.FileChooser;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;

import java.io.File;

public class FileUploadController {
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button uploadButton;
    @FXML private ToggleButton darkModeToggle;
    @FXML private StackPane dragDropArea;

    private File selectedFile;

    private boolean darkMode = true;

    @FXML
    public void initialize() {
        uploadButton.setDisable(true);
        updateStatus("Select a file to begin", Color.GRAY);
        
        if (dragDropArea != null) {
            dragDropArea.setOnDragOver(event -> {
                if (event.getGestureSource() != dragDropArea && event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
                event.consume();
            });
            dragDropArea.setOnDragEntered(event -> {
                dragDropArea.setStyle("-fx-background-color: rgba(80,120,220,0.25); -fx-background-radius: 30; -fx-border-color: #3f8efc; -fx-border-width: 3; -fx-border-radius: 30; -fx-effect: dropshadow(gaussian, #3f8efc, 30, 0.8, 0, 0);");
            });
            dragDropArea.setOnDragExited(event -> {
                dragDropArea.setStyle("-fx-background-color: rgba(60,60,80,0.75); -fx-background-radius: 30; -fx-border-color: #3f8efc; -fx-border-width: 3; -fx-border-radius: 30; -fx-effect: dropshadow(gaussian, #3f8efc, 24, 0.7, 0, 0);");
            });
            dragDropArea.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    File file = db.getFiles().get(0);
                    selectedFile = file;
                    updateStatus("Selected: " + file.getName() + " (" + getFileExtension(file) + ")", Color.DODGERBLUE);
                    uploadButton.setDisable(false);
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            });
        }
        if (darkModeToggle != null) {
            darkModeToggle.setText("🌙  Dark Mode");
            darkModeToggle.setOnAction(e -> {
                darkMode = !darkMode;
                javafx.scene.Scene scene = darkModeToggle.getScene();
                if (scene != null) {
                    if (darkMode) {
                        scene.getRoot().setStyle("-fx-background-color: linear-gradient(to bottom,#1a1a1a,#23272f);");
                        darkModeToggle.setText("🌙  Dark Mode");
                    } else {
                        scene.getRoot().setStyle("-fx-background-color: linear-gradient(to bottom,#e6eaff,#f7f8fa);");
                        darkModeToggle.setText("☀️  Light Mode");
                    }
                }
            });
        }
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

    @FXML
    private void clearFile() {
        selectedFile = null;
        updateStatus("No file selected", Color.GRAY);
        uploadButton.setDisable(true);
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

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return (lastDot == -1) ? "unknown" : name.substring(lastDot + 1).toUpperCase();
    }
}
