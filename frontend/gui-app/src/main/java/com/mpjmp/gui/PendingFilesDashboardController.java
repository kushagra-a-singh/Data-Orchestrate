package com.mpjmp.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextInputDialog;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.mpjmp.gui.util.DeviceIdentifier;

public class PendingFilesDashboardController {
    @FXML private TableView<PendingFileRow> pendingTable;
    @FXML private TableColumn<PendingFileRow, String> fileIdColumn;
    @FXML private TableColumn<PendingFileRow, String> filenameColumn;
    @FXML private TableColumn<PendingFileRow, String> metadataColumn;

    private final ObservableList<PendingFileRow> pendingRows = FXCollections.observableArrayList();
    private static final int AUTO_REFRESH_INTERVAL_MS = 15000;
    private java.util.Timer autoRefreshTimer;
    private String deviceId;

    @FXML
    public void initialize() {
        fileIdColumn.setCellValueFactory(new PropertyValueFactory<>("fileId"));
        filenameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));
        metadataColumn.setCellValueFactory(new PropertyValueFactory<>("metadata"));
        pendingTable.setItems(pendingRows);
        // Prompt or get deviceId dynamically
        this.deviceId = getDeviceIdFromContextOrPrompt();
        refreshPendingFiles();
        autoRefreshTimer = new java.util.Timer(true);
        autoRefreshTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            public void run() { refreshPendingFiles(); }
        }, AUTO_REFRESH_INTERVAL_MS, AUTO_REFRESH_INTERVAL_MS);
    }

    public void refreshPendingFiles() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8082/api/replication-status/device/" + deviceId + "/pending");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                Scanner sc = new Scanner(conn.getInputStream());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.nextLine());
                sc.close();
                JSONArray arr = new JSONArray(sb.toString());
                Platform.runLater(() -> {
                    pendingRows.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        pendingRows.add(new PendingFileRow(
                            obj.optString("fileId"),
                            obj.optString("filename"),
                            obj.optString("metadata")
                        ));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> pendingRows.clear());
            }
        }).start();
    }

    /**
     * Gets the deviceId from a context, config, or prompts the user if not set.
     * For demo, prompts via a dialog if not set.
     */
    private String getDeviceIdFromContextOrPrompt() {
        String id = DeviceIdentifier.getDeviceId();
        if (id != null && !id.isEmpty() && !"UNKNOWN".equals(id)) return id;
        // Optionally fallback to prompt
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Device ID Required");
        dialog.setHeaderText("Enter Device ID for Pending Files");
        dialog.setContentText("Device ID:");
        java.util.Optional<String> result = dialog.showAndWait();
        return result.orElse("");
    }

    public static class PendingFileRow {
        private final String fileId;
        private final String filename;
        private final String metadata;
        public PendingFileRow(String fileId, String filename, String metadata) {
            this.fileId = fileId; this.filename = filename; this.metadata = metadata;
        }
        public String getFileId() { return fileId; }
        public String getFilename() { return filename; }
        public String getMetadata() { return metadata; }
    }
}
