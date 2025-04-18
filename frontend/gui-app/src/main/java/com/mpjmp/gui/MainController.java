package com.mpjmp.gui;

import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import com.mpjmp.gui.utils.ErrorDialog;

public class MainController {
    @FXML private MenuItem syncRulesMenuItem;
    @FXML private MenuItem syncHistoryMenuItem;
    @FXML private MenuItem metricsMenuItem;
    
    @FXML 
    public void showSyncRules() {
        // Implementation
    }
    
    @FXML
    public void showSyncHistory() {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/views/sync_history.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Sync History");
            stage.show();
        } catch (IOException e) {
            showError("Failed to open sync history");
        }
    }
    
    @FXML
    public void showMetricsDashboard() {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/views/metrics_dashboard.fxml"));
            stage.setScene(new Scene(root));
            stage.setTitle("Performance Metrics");
            stage.show();
        } catch (IOException e) {
            showError("Failed to open metrics dashboard");
        }
    }
    
    private void showError(String message) {
        ErrorDialog.show(message);
    }
}
