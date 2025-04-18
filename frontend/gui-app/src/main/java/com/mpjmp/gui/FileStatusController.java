package com.mpjmp.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import java.util.Timer;
import java.util.TimerTask;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.io.File;
import java.io.FileInputStream;
import javafx.application.Platform;
import com.mpjmp.gui.utils.ErrorDialog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FileStatusController {
    @FXML private ProgressBar syncProgress;
    @FXML private Label statusLabel;
    
    private Timer progressTimer;
    private GridFSBucket gridFsBucket;
    private String currentUser;
    
    public void trackReplication(String fileId, String deviceId) {
        progressTimer = new Timer();
        progressTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                updateProgress(fileId, deviceId);
            }
        }, 0, 1000); // Update every second
    }
    
    private void updateProgress(String fileId, String deviceId) {
        try {
            GridFSFile file = gridFsBucket.find(new Document("_id", fileId)).first();
            if (file != null) {
                JsonNode progress = new ObjectMapper().readTree(file.getMetadata().toJson());
                Platform.runLater(() -> {
                    syncProgress.setProgress(progress.get("progress").asDouble());
                    statusLabel.setText(String.format("Syncing: %.0f%%", 
                        progress.get("progress").asDouble() * 100));
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> 
                statusLabel.setText("Progress update failed")
            );
        }
    }
    
    public void uploadFile(File file) {
        try {
            GridFSUploadOptions options = new GridFSUploadOptions()
                .metadata(new Document("uploadedBy", currentUser));
            
            ObjectId fileId = gridFsBucket.uploadFromStream(
                file.getName(),
                new FileInputStream(file),
                options
            );
            
            Platform.runLater(() -> 
                statusLabel.setText("Uploaded: " + fileId)
            );
        } catch (Exception e) {
            Platform.runLater(() -> 
                ErrorDialog.show("Upload failed")
            );
        }
    }
}
