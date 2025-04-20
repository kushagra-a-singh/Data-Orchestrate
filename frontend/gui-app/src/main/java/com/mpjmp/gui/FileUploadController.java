package com.mpjmp.gui;

import com.mpjmp.gui.api.FileUploadService;
import com.mpjmp.gui.api.ProgressTrackingService;
import com.mpjmp.gui.api.AtlasUploadService;
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
import java.util.Arrays;
import java.util.List;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.json.JSONObject;
import com.mpjmp.gui.util.DeviceIdentifier;
import com.mpjmp.gui.util.BackendDeviceIdProvider;

public class FileUploadController {
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button uploadButton;
    @FXML private ToggleButton darkModeToggle;
    @FXML private StackPane dragDropArea;

    private File selectedFile;

    private boolean darkMode = true;

    // --- SUPPORTED FILE TYPES ---
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("PDF", "DOCX", "XLSX", "JPG", "JPEG", "PNG", "ZIP", "TXT", "CSV", "MP4", "MP3");

    private boolean isSupportedFileType(File file) {
        String ext = getFileExtension(file);
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

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
                    if (!isSupportedFileType(file)) {
                        showErrorDialog("Unsupported File Type", "Supported types: " + String.join(", ", SUPPORTED_EXTENSIONS));
                        selectedFile = null;
                        updateStatus("No file selected", Color.GRAY);
                        uploadButton.setDisable(true);
                        success = false;
                    } else {
                        selectedFile = file;
                        updateStatus("Selected: " + file.getName() + " (" + getFileExtension(file) + ")", Color.DODGERBLUE);
                        uploadButton.setDisable(false);
                        success = true;
                    }
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
            if (!isSupportedFileType(selectedFile)) {
                showErrorDialog("Unsupported File Type", "Supported types: " + String.join(", ", SUPPORTED_EXTENSIONS));
                selectedFile = null;
                updateStatus("No file selected", Color.GRAY);
                uploadButton.setDisable(true);
                return;
            }
            updateStatus("Selected: " + selectedFile.getName(), Color.GREEN);
            uploadButton.setDisable(false);
        } else {
            updateStatus("No file selected", Color.GRAY);
            uploadButton.setDisable(true);
        }
    }

    @FXML
    public void uploadFile() {
        if (selectedFile == null) return;
        uploadButton.setDisable(true);
        progressBar.setProgress(-1);
        new Thread(() -> {
            try {
                String uploadedBy = System.getProperty("USER_NAME", System.getProperty("user.name", "unknown"));
                String deviceId = BackendDeviceIdProvider.getBackendDeviceId();
                if (deviceId == null || deviceId.isEmpty()) {
                    // fallback to legacy DeviceIdentifier if backend not available
                    deviceId = DeviceIdentifier.getDeviceId();
                }
                String deviceName = DeviceIdentifier.getDeviceName();
                // Debug print to verify deviceId and deviceName
                System.out.println("[DEBUG] Using deviceId for upload/replication: " + deviceId);
                System.out.println("[DEBUG] Using deviceName: " + deviceName);
                Platform.runLater(() -> {
                    updateStatus("Device ID: " + deviceId + ", Device Name: " + deviceName, Color.DARKBLUE);
                });
                String uploadResponse = FileUploadService.uploadFile(selectedFile, uploadedBy, deviceId);
                String fileId = extractFileId(uploadResponse);
                String replicateResponse = FileUploadService.replicateFile(
                    fileId,
                    selectedFile.getName(),
                    deviceId,
                    "http://localhost:8081"
                );
                Platform.runLater(() -> {
                    updateStatus("Upload & replication successful! File ID: " + fileId, Color.GREEN);
                    progressBar.setProgress(1);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showErrorDialog("Upload/Replication failed", e.getMessage());
                    updateStatus("Failed: " + e.getMessage(), Color.RED);
                    uploadButton.setDisable(false);
                });
            }
        }).start();
    }

    // Helper to extract fileId from upload response JSON
    private String extractFileId(String response) {
        try {
            org.json.JSONObject obj = new org.json.JSONObject(response);
            if (obj.has("id")) return obj.getString("id");
            if (obj.has("fileId")) return obj.getString("fileId");
            return response;
        } catch (Exception e) {
            return response;
        }
    }

    @FXML
    private void clearFile() {
        selectedFile = null;
        updateStatus("No file selected", Color.GRAY);
        uploadButton.setDisable(true);
    }

    @FXML
    public void showSyncRules() {
        // TODO: Implement sync rules dialog or navigation
        showErrorDialog("Not implemented", "Sync Rules dialog is not implemented yet.");
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
