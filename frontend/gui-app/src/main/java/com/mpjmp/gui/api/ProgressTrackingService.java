package com.mpjmp.gui.api;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ProgressTrackingService {
    private static final String PROGRESS_URL = "http://localhost:8083/progress/";

    public static String getProcessingProgress(String fileId) {
        try {
            URL url = new URL(PROGRESS_URL + fileId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                Scanner scanner = new Scanner(connection.getInputStream()).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "No progress data.";
            } else {
                return "Error fetching progress: " + connection.getResponseCode();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
