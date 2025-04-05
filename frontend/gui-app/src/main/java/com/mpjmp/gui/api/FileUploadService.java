package com.mpjmp.gui.api;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

public class FileUploadService {
    private static final String UPLOAD_URL = "http://file-upload-service:8081/upload";
    private static final String BOUNDARY = "----WebKitFormBoundary" + System.currentTimeMillis();

public static String uploadFile(File file, ProgressTrackingService progressService) {

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
            connection.setRequestProperty("Content-Length", String.valueOf(file.length()));
            progressService.startTracking(file.length()); // Start tracking progress

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

            try (OutputStream outputStream = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

                writer.append("--").append(BOUNDARY).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: application/octet-stream\r\n");
                writer.append("\r\n");
                writer.flush();

                Files.copy(file.toPath(), outputStream);
                outputStream.flush();

                writer.append("\r\n");
                writer.append("--").append(BOUNDARY).append("--\r\n");
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            progressService.completeTracking(); // Complete tracking after upload

            if (responseCode == 200) {
                return "Upload successful!";
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String errorMessage = reader.readLine();
                    return "Upload failed! Response Code: " + responseCode + ", Error: " + errorMessage;
                }
            }
        } catch (IOException e) {
            return "Error: " + e.getMessage() + ". Please ensure the file-upload-service is running.";
        }
    }
}
