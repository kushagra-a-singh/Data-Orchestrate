package com.mpjmp.gui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class LogPanel extends VBox {
    private static LogPanel instance;
    private final TextArea logArea;
    private LogPanel() {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(10);
        getChildren().add(logArea);
    }
    public static LogPanel getInstance() {
        if (instance == null) instance = new LogPanel();
        return instance;
    }
    public void appendLog(String log) {
        Platform.runLater(() -> logArea.appendText(log + "\n"));
    }
}
