package com.mpjmp.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import java.util.List;

public class DeviceStatusPanel extends VBox {
    private static DeviceStatusPanel instance;
    private final TableView<DeviceStatus> table;
    private final ObservableList<DeviceStatus> data;
    private DeviceStatusPanel() {
        data = FXCollections.observableArrayList();
        table = new TableView<>(data);
        TableColumn<DeviceStatus, String> nameCol = new TableColumn<>("Device Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        TableColumn<DeviceStatus, String> idCol = new TableColumn<>("Device ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
        TableColumn<DeviceStatus, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<DeviceStatus, String> reachCol = new TableColumn<>("Reachable");
        reachCol.setCellValueFactory(new PropertyValueFactory<>("reachable"));
        table.getColumns().addAll(nameCol, idCol, statusCol, reachCol);
        getChildren().add(table);
    }
    public static DeviceStatusPanel getInstance() {
        if (instance == null) instance = new DeviceStatusPanel();
        return instance;
    }
    public void updateDevices(List<DeviceStatus> devices) {
        Platform.runLater(() -> {
            data.setAll(devices);
        });
    }
    public TableView<DeviceStatus> getTable() { return table; }
}
