package com.mpjmp.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.net.http.*;
import java.net.URI;

public class BulkOperationsController {
    @FXML private TableView<FileProgress> fileTable;
    @FXML private ProgressBar overallProgress;
    
    public void initialize() {
        fileTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(FileProgress item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setStyle(item.getProgress() == 1.0 ? 
                        "-fx-background-color: #e8f5e9;" : "");
                }
            }
        });
    }
    
    public void trackBatch(String batchId) {
        // Implementation to track batch progress
    }
}
