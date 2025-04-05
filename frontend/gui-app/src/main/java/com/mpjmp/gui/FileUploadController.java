package com.mpjmp.gui;

import com.mpjmp.gui.api.FileUploadService;
import com.mpjmp.gui.api.ProgressTrackingService;
import com.mpjmp.gui.api.StorageService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class FileUploadController {
    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB
    private static final Map<String, String> ALLOWED_TYPES = new HashMap<String, String>() {{
        put(".txt", "Text files");
        put(".pdf", "PDF documents");
        put(".doc", "Word documents");
        put(".docx", "Word documents");
        put(".csv", "CSV files");
        put(".json", "JSON files");
        put(".xml", "XML files");
        put(".jpg", "JPEG images");
        put(".jpeg", "JPEG images");
        put(".png", "PNG images");
        put(".gif", "GIF images");
        put(".bmp", "BMP images");
    }};
    private static final int MAX_RETRIES = 3;
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#.##");
    
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button uploadButton;
    @FXML private Button chooseButton;
    @FXML private Label fileInfoLabel;
    
    private File selectedFile;
    private final ScheduledExecutorService executorService;
    private volatile boolean isUploading;
    
    public FileUploadController() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }
    
    @FXML
    public void initialize() {
        progressBar.setProgress(0);
        uploadButton.setDisable(true);
        fileInfoLabel.setText("");
        
        // Apply CSS styling
        statusLabel.getStyleClass().add("status-label");
        uploadButton.getStyleClass().add("action-button");
        chooseButton.getStyleClass().add("action-button");
        progressBar.getStyleClass().add("progress-bar");
        fileInfoLabel.getStyleClass().add("info-text");
    }
    
    @FXML
    public void chooseFile() {
        if (isUploading) {
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        
        // Add file filters for each allowed type
        ALLOWED_TYPES.forEach((extension, description) -> 
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(description, "*" + extension)
            )
        );
        
        selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            if (!validateFile(selectedFile)) {
                showError("Invalid file selected. Please check size and type.");
                selectedFile = null;
                uploadButton.setDisable(true);
                fileInfoLabel.setText("");
                return;
            }
            updateStatus("Selected: " + selectedFile.getName(), Color.BLACK);
            updateFileInfo(selectedFile);
            uploadButton.setDisable(false);
        }
    }
    
    @FXML
    public void uploadFile() {
        if (selectedFile == null || isUploading) {
            return;
        }
        
        isUploading = true;
        updateUIState(true);
        
        CompletableFuture.runAsync(() -> {
            AtomicInteger remainingRetries = new AtomicInteger(MAX_RETRIES);
            
            while (remainingRetries.get() > 0) {
                try {
                    ProgressTrackingService progressService = new ProgressTrackingService();
                    progressService.setOnProgress(progress -> {
                        Platform.runLater(() -> {
                            progressBar.setProgress(progress);
                            updateStatus(String.format("Uploading... %.1f%%", progress * 100), Color.BLACK);
                        });
                    });
                    
                    // First upload to file-upload-service
                    String uploadResponse = FileUploadService.uploadFile(selectedFile, progressService);
                    
                    // Then register with storage-service
                    String storageResponse = StorageService.registerFile(uploadResponse);
                    
                    Platform.runLater(() -> {
                        showSuccess("Upload successful: " + storageResponse);
                        resetUploadState();
                    });
                    break;
                } catch (Exception e) {
                    log.error("Upload failed", e);
                    int retries = remainingRetries.decrementAndGet();
                    if (retries == 0) {
                        Platform.runLater(() -> {
                            showError("Upload failed: " + e.getMessage());
                            resetUploadState();
                        });
                    } else {
                        final int attemptsLeft = retries;
                        Platform.runLater(() -> 
                            showWarning("Retrying upload... Attempts remaining: " + attemptsLeft)
                        );
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }, executorService);
    }
    
    private boolean validateFile(File file) {
        if (file.length() > MAX_FILE_SIZE) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        return ALLOWED_TYPES.keySet().stream()
            .anyMatch(fileName::endsWith);
    }
    
    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setTextFill(color);
    }
    
    private void showError(String message) {
        updateStatus("❌ " + message, Color.RED);
    }
    
    private void showWarning(String message) {
        updateStatus("⚠️ " + message, Color.ORANGE);
    }
    
    private void showSuccess(String message) {
        updateStatus("✅ " + message, Color.GREEN);
    }
    
    private void updateFileInfo(File file) {
        String size = formatFileSize(file.length());
        fileInfoLabel.setText(String.format("Size: %s", size));
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return SIZE_FORMAT.format(bytes / 1024.0) + " KB";
        return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
    }
    
    private void updateUIState(boolean uploading) {
        uploadButton.setDisable(uploading);
        chooseButton.setDisable(uploading);
        if (!uploading) {
            progressBar.setProgress(0);
        }
    }
    
    private void resetUploadState() {
        isUploading = false;
        updateUIState(false);
        selectedFile = null;
        fileInfoLabel.setText("");
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
