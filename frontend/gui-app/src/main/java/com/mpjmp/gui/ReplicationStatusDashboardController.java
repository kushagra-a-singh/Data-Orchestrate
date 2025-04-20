package com.mpjmp.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ProgressBar;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.mpjmp.gui.util.DeviceIdentifier;
import com.mpjmp.gui.util.BackendDeviceIdProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ReplicationStatusDashboardController {
    @FXML private TableView<ReplicationStatusRow> statusTable;
    @FXML private TableColumn<ReplicationStatusRow, String> fileIdColumn;
    @FXML private TableColumn<ReplicationStatusRow, String> deviceIdColumn;
    @FXML private TableColumn<ReplicationStatusRow, String> timestampColumn;
    @FXML private ProgressBar progressBar;

    private final ObservableList<ReplicationStatusRow> statusRows = FXCollections.observableArrayList();

    private static final int AUTO_REFRESH_INTERVAL_MS = 15000; // 15 seconds
    private java.util.Timer autoRefreshTimer;

    private String deviceId;

    @FXML
    public void initialize() {
        fileIdColumn.setCellValueFactory(new PropertyValueFactory<>("fileId"));
        deviceIdColumn.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        statusTable.setItems(statusRows);
        // Prompt or get deviceId dynamically
        this.deviceId = getDeviceIdFromContextOrPrompt();
        refreshReplicationStatus();
        // Auto-refresh
        autoRefreshTimer = new java.util.Timer(true);
        autoRefreshTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            public void run() { refreshReplicationStatus(); }
        }, AUTO_REFRESH_INTERVAL_MS, AUTO_REFRESH_INTERVAL_MS);
    }

    // All status and progress should be fetched via HTTP endpoints.
    // WebSocket-based logic is deprecated.
    public void refreshReplicationStatus() {
        new Thread(() -> {
            try {
                URL url = new URL(getReplicationStatusUrl(deviceId));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                Scanner sc = new Scanner(conn.getInputStream());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.nextLine());
                sc.close();
                JSONArray arr = new JSONArray(sb.toString());
                Platform.runLater(() -> {
                    statusRows.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        statusRows.add(new ReplicationStatusRow(
                            obj.optString("fileId"),
                            obj.optString("deviceId"),
                            obj.optString("timestamp")
                        ));
                    }
                    // Progress: completed/total
                    double progress = arr.length() == 0 ? 1.0 : (double) arr.length() / (arr.length() + getPendingCount());
                    progressBar.setProgress(progress);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusRows.clear();
                    progressBar.setProgress(0);
                });
            }
        }).start();
    }

    private int getPendingCount() {
        try {
            URL url = new URL(getPendingCountUrl(deviceId));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            Scanner sc = new Scanner(conn.getInputStream());
            StringBuilder sb = new StringBuilder();
            while (sc.hasNext()) sb.append(sc.nextLine());
            sc.close();
            JSONArray arr = new JSONArray(sb.toString());
            return arr.length();
        } catch (Exception e) {
            return 0;
        }
    }

    private String getReplicationStatusUrl(String deviceId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("devices.json");
            List<Map<String, String>> allDevices = mapper.readValue(is, new TypeReference<List<Map<String, String>>>() {});
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(System.getProperty("DEVICE_NAME", System.getenv("DEVICE_NAME")))).findFirst().orElse(null);
            if (self != null) {
                return "http://" + self.get("ip") + ":" + self.get("port") + "/api/replication-status/device/" + deviceId;
            } else {
                throw new RuntimeException("Self device not found in device config");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load device config", e);
        }
    }

    private String getPendingCountUrl(String deviceId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("devices.json");
            List<Map<String, String>> allDevices = mapper.readValue(is, new TypeReference<List<Map<String, String>>>() {});
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(System.getProperty("DEVICE_NAME", System.getenv("DEVICE_NAME")))).findFirst().orElse(null);
            if (self != null) {
                return "http://" + self.get("ip") + ":" + self.get("port") + "/api/replication-status/device/" + deviceId + "/pending";
            } else {
                throw new RuntimeException("Self device not found in device config");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load device config", e);
        }
    }

    /**
     * Gets the deviceId from a context, config, or prompts the user if not set.
     * For demo, prompts via a dialog if not set.
     */
    private String getDeviceIdFromContextOrPrompt() {
        String id = BackendDeviceIdProvider.getBackendDeviceId();
        if (id != null && !id.isEmpty() && !"UNKNOWN".equals(id)) return id;
        id = DeviceIdentifier.getDeviceId();
        if (id != null && !id.isEmpty() && !"UNKNOWN".equals(id)) return id;
        // Optionally fallback to prompt
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Device ID Required");
        dialog.setHeaderText("Enter Device ID for Replication Status");
        dialog.setContentText("Device ID:");
        java.util.Optional<String> result = dialog.showAndWait();
        return result.orElse("");
    }

    public static class ReplicationStatusRow {
        private final String fileId;
        private final String deviceId;
        private final String timestamp;
        public ReplicationStatusRow(String fileId, String deviceId, String timestamp) {
            this.fileId = fileId; this.deviceId = deviceId; this.timestamp = timestamp;
        }
        public String getFileId() { return fileId; }
        public String getDeviceId() { return deviceId; }
        public String getTimestamp() { return timestamp; }
    }
}
