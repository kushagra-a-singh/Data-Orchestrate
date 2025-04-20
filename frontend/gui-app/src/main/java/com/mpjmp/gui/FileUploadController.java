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
                // Try to get MongoDB URI from System properties first (set by Main.java)
                String uri = System.getProperty("MONGODB_URI");
                
                // Fallback to environment variable if not found in properties
                if (uri == null || uri.isEmpty()) {
                    uri = System.getenv("MONGODB_URI");
                }
                
                // If still not found, throw an error
                if (uri == null || uri.isEmpty()) {
                    throw new RuntimeException("MONGODB_URI environment variable is not set.");
                }
                
                MongoClient mongoClient = MongoClients.create(uri);
                AtlasUploadService service = new AtlasUploadService(mongoClient);
                String fileId = service.uploadDirectToAtlas(selectedFile);
                Platform.runLater(() -> {
                    updateStatus("Upload successful! ID: " + fileId, Color.GREEN);
                    progressBar.setProgress(1);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showErrorDialog("Upload failed", e.getMessage());
                    updateStatus("Upload failed: " + e.getMessage(), Color.RED);
                    uploadButton.setDisable(false);
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
