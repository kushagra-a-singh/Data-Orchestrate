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
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class FileStatusController {
    @FXML private ProgressBar syncProgress;
    @FXML private Label statusLabel;
    
    private Timer progressTimer;
    private GridFSBucket gridFsBucket;
    private String currentUser;
    
    public void trackReplication(String fileId, String deviceId) {
        progressTimer = new Timer();
        final String fileIdFinal = fileId;
        final String deviceIdFinal = deviceId;
        progressTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                updateProgress(fileIdFinal, deviceIdFinal);
            }
        }, 0, 1000); // Update every second
    }
    
    // All replication progress/status logic should use HTTP endpoints.
    // WebSocket or Kafka logic is deprecated.
    private void updateProgress(String fileId, String deviceId) {
        final String fileIdFinal = fileId;
        try {
            // Use device info from devices.json for replication status URL
            URL url = new URL(getReplicationStatusUrl(deviceId));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            Scanner sc = new Scanner(conn.getInputStream());
            StringBuilder sb = new StringBuilder();
            while (sc.hasNext()) sb.append(sc.nextLine());
            sc.close();
            JSONArray arr = new JSONArray(sb.toString());
            // Find this fileId
            boolean replicated = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (fileIdFinal.equals(obj.optString("fileId"))) {
                    replicated = true;
                    break;
                }
            }
            final boolean replicatedFinal = replicated;
            Platform.runLater(() -> {
                syncProgress.setProgress(replicatedFinal ? 1.0 : 0.0);
                statusLabel.setText(replicatedFinal ? "Replication complete" : "Syncing...");
            });
        } catch (Exception e) {
            final String errorMsg = e.getMessage();
            Platform.runLater(() -> statusLabel.setText("Progress update failed: " + errorMsg));
        }
    }
    
    private String getReplicationStatusUrl(String deviceId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("devices.json");
            List<Map<String, String>> allDevices = mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});
            String selfDeviceName = System.getProperty("DEVICE_NAME", System.getenv("DEVICE_NAME"));
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(selfDeviceName)).findFirst().orElse(null);
            Map<String, String> peer = allDevices.stream().filter(d -> d.get("name").equals(deviceId)).findFirst().orElse(null);
            if (peer != null) {
                return "http://" + peer.get("ip") + ":" + peer.get("port") + "/api/replication-status/device/" + deviceId;
            }
            if (self != null) {
                return "http://" + self.get("ip") + ":" + self.get("port") + "/api/replication-status/device/" + deviceId;
            } else {
                throw new RuntimeException("Self device not found in device config");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load device config", e);
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
            final String fileIdStr = fileId.toString();
            Platform.runLater(() -> 
                statusLabel.setText("Uploaded: " + fileIdStr)
            );
        } catch (Exception e) {
            Platform.runLater(() -> 
                ErrorDialog.show("Upload failed")
            );
        }
    }
}
