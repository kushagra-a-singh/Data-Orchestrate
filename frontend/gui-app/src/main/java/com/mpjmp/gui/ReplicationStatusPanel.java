package com.mpjmp.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class ReplicationStatusPanel extends VBox {
    private static ReplicationStatusPanel instance;
    private final TableView<ReplicationStatus> table;
    private final ObservableList<ReplicationStatus> data;
    private ReplicationStatusPanel() {
        data = FXCollections.observableArrayList();
        table = new TableView<>(data);
        TableColumn<ReplicationStatus, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        TableColumn<ReplicationStatus, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<ReplicationStatus, String> deviceCol = new TableColumn<>("Device");
        deviceCol.setCellValueFactory(new PropertyValueFactory<>("targetDeviceId"));
        table.getColumns().addAll(fileCol, statusCol, deviceCol);
        getChildren().add(table);
    }
    public static ReplicationStatusPanel getInstance() {
        if (instance == null) instance = new ReplicationStatusPanel();
        return instance;
    }
    public void updateStatus(String fileName, String status, String deviceId) {
        Platform.runLater(() -> {
            for (ReplicationStatus rs : data) {
                if (rs.getFileName().equals(fileName) && rs.getTargetDeviceId().equals(deviceId)) {
                    rs.setStatus(status);
                    table.refresh();
                    return;
                }
            }
            data.add(new ReplicationStatus(fileName, status, deviceId));
        });
    }
    public TableView<ReplicationStatus> getTable() { return table; }
}

class ReplicationStatus {
    private String fileName;
    private String status;
    private String targetDeviceId;
    public ReplicationStatus(String fileName, String status, String deviceId) {
        this.fileName = fileName;
        this.status = status;
        this.targetDeviceId = deviceId;
    }
    public String getFileName() { return fileName; }
    public String getStatus() { return status; }
    public String getTargetDeviceId() { return targetDeviceId; }
    public void setStatus(String status) { this.status = status; }
}
