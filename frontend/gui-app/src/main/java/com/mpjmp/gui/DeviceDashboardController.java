package com.mpjmp.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class DeviceDashboardController {
    @FXML private TableView<DeviceInfoRow> deviceTable;
    @FXML private TableColumn<DeviceInfoRow, String> idColumn;
    @FXML private TableColumn<DeviceInfoRow, String> nameColumn;
    @FXML private TableColumn<DeviceInfoRow, String> statusColumn;
    @FXML private TableColumn<DeviceInfoRow, String> lastSeenColumn;
    @FXML private Label refreshLabel;

    private final ObservableList<DeviceInfoRow> deviceRows = FXCollections.observableArrayList();

    private static final int AUTO_REFRESH_INTERVAL_MS = 15000; // 15 seconds
    private java.util.Timer autoRefreshTimer;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        lastSeenColumn.setCellValueFactory(new PropertyValueFactory<>("lastSeen"));
        deviceTable.setItems(deviceRows);
        refreshDeviceTable();
        // Auto-refresh
        autoRefreshTimer = new java.util.Timer(true);
        autoRefreshTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            public void run() { refreshDeviceTable(); }
        }, AUTO_REFRESH_INTERVAL_MS, AUTO_REFRESH_INTERVAL_MS);
    }

    public void refreshDeviceTable() {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8082/api/devices");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                Scanner sc = new Scanner(conn.getInputStream());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.nextLine());
                sc.close();
                JSONArray arr = new JSONArray(sb.toString());
                Platform.runLater(() -> {
                    deviceRows.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        deviceRows.add(new DeviceInfoRow(
                            obj.optString("id", obj.optString("deviceId")),
                            obj.optString("name", "UNKNOWN"),
                            obj.optString("status", "UNKNOWN"),
                            obj.optString("lastSeen", "-")
                        ));
                    }
                    refreshLabel.setText("Last updated: " + java.time.LocalTime.now());
                });
            } catch (Exception e) {
                Platform.runLater(() -> refreshLabel.setText("Failed to fetch devices"));
            }
        }).start();
    }

    public static class DeviceInfoRow {
        private final String id;
        private final String name;
        private final String status;
        private final String lastSeen;
        public DeviceInfoRow(String id, String name, String status, String lastSeen) {
            this.id = id; this.name = name; this.status = status; this.lastSeen = lastSeen;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getLastSeen() { return lastSeen; }
    }
}
