package com.mpjmp.gui.api;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgressTrackingService {
    private long totalSize;
    private long uploadedSize;
    private Consumer<Double> progressCallback;

    public void setOnProgress(Consumer<Double> callback) {
        this.progressCallback = callback;
    }

    public void startTracking(long totalSize) {
        this.totalSize = totalSize;
        this.uploadedSize = 0;
        log.info("Tracking started for file of size: {}", totalSize);
        notifyProgress();
    }

    public void updateProgress(long bytesUploaded) {
        this.uploadedSize += bytesUploaded;
        notifyProgress();
        log.debug("Uploaded: {} of {} bytes ({:.2f}%)", uploadedSize, totalSize, getProgressPercentage());
    }

    public void completeTracking() {
        this.uploadedSize = this.totalSize;
        notifyProgress();
        log.info("Tracking completed for file.");
    }

    private void notifyProgress() {
        if (progressCallback != null) {
            progressCallback.accept(getProgressPercentage() / 100.0);
        }
    }

    private double getProgressPercentage() {
        return totalSize > 0 ? (uploadedSize * 100.0 / totalSize) : 0.0;
    }

    private static final String PROGRESS_URL = "http://localhost:8083/progress/";

    public static String getProcessingProgress(String fileId) {
        try {
            URL url = new URL(PROGRESS_URL + fileId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == 200) {
                try (Scanner scanner = new Scanner(connection.getInputStream()).useDelimiter("\\A")) {
                    return scanner.hasNext() ? scanner.next() : "No progress data.";
                }
            } else {
                log.error("Error fetching progress. Response code: {}", connection.getResponseCode());
                return "Error fetching progress: " + connection.getResponseCode();
            }
        } catch (Exception e) {
            log.error("Error fetching progress", e);
            return "Error: " + e.getMessage();
        }
    }
}
