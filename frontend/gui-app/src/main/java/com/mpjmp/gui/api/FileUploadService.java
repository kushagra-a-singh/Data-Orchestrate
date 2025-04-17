package com.mpjmp.gui.api;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class FileUploadService {
    private static final String UPLOAD_URL = "http://localhost:8081/api/files/upload";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String LINE_FEED = "\r\n";

    public static String uploadFile(File file) {
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();
        
        try {
            URL url = new URL(UPLOAD_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept", "application/json");
            
            try (OutputStream outputStream = connection.getOutputStream()) {
                // Get the file content
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                
                // Write file part
                outputStream.write(("--" + boundary + LINE_FEED).getBytes(StandardCharsets.UTF_8));
                outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + LINE_FEED).getBytes(StandardCharsets.UTF_8));
                outputStream.write(("Content-Type: application/octet-stream" + LINE_FEED + LINE_FEED).getBytes(StandardCharsets.UTF_8));
                outputStream.write(fileBytes);
                outputStream.write(LINE_FEED.getBytes(StandardCharsets.UTF_8));
                
                // Write uploadedBy part
                String username = System.getProperty("user.name");
                outputStream.write(("--" + boundary + LINE_FEED).getBytes(StandardCharsets.UTF_8));
                outputStream.write(("Content-Disposition: form-data; name=\"uploadedBy\"" + LINE_FEED + LINE_FEED).getBytes(StandardCharsets.UTF_8));
                outputStream.write(username.getBytes(StandardCharsets.UTF_8));
                outputStream.write(LINE_FEED.getBytes(StandardCharsets.UTF_8));
                
                // Write deviceName part
                String deviceName = System.getenv("COMPUTERNAME");
                if (deviceName == null || deviceName.isEmpty()) {
                    deviceName = "Unknown-Device";
                }
                outputStream.write(("--" + boundary + LINE_FEED).getBytes(StandardCharsets.UTF_8));
                outputStream.write(("Content-Disposition: form-data; name=\"deviceName\"" + LINE_FEED + LINE_FEED).getBytes(StandardCharsets.UTF_8));
                outputStream.write(deviceName.getBytes(StandardCharsets.UTF_8));
                outputStream.write(LINE_FEED.getBytes(StandardCharsets.UTF_8));
                
                // Close the multipart form
                outputStream.write(("--" + boundary + "--" + LINE_FEED).getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
            
            // Get the response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                StringBuilder response = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                // Parse JSON response
                try {
                    JsonNode jsonNode = objectMapper.readTree(response.toString());
                    String fileId = jsonNode.get("id").asText();
                    return "Upload successful! File ID: " + fileId;
                } catch (Exception e) {
                    return "Upload successful! Response: " + response.toString();
                }
            } else {
                // Read error response
                StringBuilder errorResponse = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
                
                return "Upload failed! Response Code: " + responseCode + ", Body: " + errorResponse.toString();
            }
        } catch (IOException e) {
            if (e.getMessage().contains("Connection refused")) {
                return "Error: File upload service is not running. Please start the file-upload service first.";
            }
            return "Error: " + e.getMessage();
        }
    }
}
