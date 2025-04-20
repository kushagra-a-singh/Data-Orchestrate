package com.mpjmp.gui;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpjmp.gui.util.BackendDeviceIdProvider;
import com.mpjmp.gui.util.DeviceIdentifier;
import com.dataorchestrate.common.DeviceConfigUtil;

public class SyncRulesController {
    @FXML private TableView<SyncRule> rulesTable;
    @FXML private TextField pathPatternField;
    @FXML private ComboBox<SyncDirection> directionCombo;
    @FXML private ComboBox<ConflictResolution> resolutionCombo;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    @FXML
    public void initialize() {
        loadRules();
        
        directionCombo.getItems().setAll(SyncDirection.values());
        resolutionCombo.getItems().setAll(ConflictResolution.values());
    }
    
    private void loadRules() {
        try {
            // Use devices.json for all device info
            List<Map<String, String>> allDevices = DeviceConfigUtil.getAllDevices();
            String selfDeviceName = DeviceConfigUtil.getSelfDeviceName();
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(selfDeviceName)).findFirst().orElse(null);
            if (self != null) {
                String url = "http://" + self.get("ip") + ":" + self.get("port") + "/api/sync-config/" + getDeviceId();
                HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                );
                SyncRule[] rules = new ObjectMapper().readValue(response.body(), SyncRule[].class);
                rulesTable.getItems().setAll(rules);
            } else {
                showError("Self device not found in config");
            }
        } catch (Exception e) {
            showError("Failed to load rules: " + e.getMessage());
        }
    }
    
    private String getDeviceId() {
        String id = BackendDeviceIdProvider.getBackendDeviceId();
        if (id != null && !id.isEmpty() && !"UNKNOWN".equals(id)) return id;
        id = DeviceIdentifier.getDeviceId();
        if (id != null && !id.isEmpty() && !"UNKNOWN".equals(id)) return id;
        return "demo-device-id";
    }
    
    private void showError(String message) {
        // TODO: Implement error display logic, e.g., ErrorDialog.show(message)
        System.err.println(message);
    }
    
    @FXML 
    public void saveRule() {
        // Validation and save logic
    }
}
