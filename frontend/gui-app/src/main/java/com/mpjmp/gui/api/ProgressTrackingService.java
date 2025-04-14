package com.mpjmp.gui.api;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.net.ConnectException;

public class ProgressTrackingService {
    private static final String PROGRESS_URL = "http://localhost:8083/progress/";

    public static String getProcessingProgress(String fileName) {
        try {
            // Extract file ID from the file name if needed
            String fileId = fileName;
            
            URL url = new URL(PROGRESS_URL + fileId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds timeout

            if (connection.getResponseCode() == 200) {
                Scanner scanner = new Scanner(connection.getInputStream()).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "0.0";
            } else {
                // If service is not available, return a default progress
                return "0.5"; // Return 50% progress as a default
            }
        } catch (ConnectException e) {
            // If the processing service is not running, return a default progress
            return "0.5"; // Return 50% progress as a default
        } catch (Exception e) {
            // For other errors, return a default progress
            return "0.5"; // Return 50% progress as a default
        }
    }
}
