package com.mpjmp.gui;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;

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
            HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8082/api/sync-config/" + getDeviceId()))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            
            SyncRule[] rules = new ObjectMapper().readValue(response.body(), SyncRule[].class);
            rulesTable.getItems().setAll(rules);
        } catch (Exception e) {
            showError("Failed to load rules: " + e.getMessage());
        }
    }
    
    private String getDeviceId() {
        // TODO: Implement device ID retrieval logic
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
