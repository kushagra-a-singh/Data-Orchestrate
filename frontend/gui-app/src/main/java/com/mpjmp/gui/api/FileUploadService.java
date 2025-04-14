package com.mpjmp.gui.api;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.net.ConnectException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class FileUploadService {
    private static final String UPLOAD_URL = "http://localhost:8081/api/files/upload";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String uploadFile(File file) {
        try {
            // Create multipart form data
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String fileName = file.getName();
            
            // Build multipart form data
            String boundary = "----" + System.currentTimeMillis();
            StringBuilder formData = new StringBuilder();
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
            formData.append("Content-Type: application/octet-stream\r\n\r\n");
            
            // Combine form data with file content
            byte[] formDataBytes = formData.toString().getBytes();
            byte[] boundaryBytes = ("\r\n--" + boundary + "--\r\n").getBytes();
            
            byte[] requestBody = new byte[formDataBytes.length + fileBytes.length + boundaryBytes.length];
            System.arraycopy(formDataBytes, 0, requestBody, 0, formDataBytes.length);
            System.arraycopy(fileBytes, 0, requestBody, formDataBytes.length, fileBytes.length);
            System.arraycopy(boundaryBytes, 0, requestBody, formDataBytes.length + fileBytes.length, boundaryBytes.length);

            // Create HTTP request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UPLOAD_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse the JSON response
                try {
                    JsonNode jsonNode = objectMapper.readTree(response.body());
                    String fileId = jsonNode.get("id").asText();
                    return "Upload successful! File ID: " + fileId;
                } catch (Exception e) {
                    return "Upload successful! Response: " + response.body();
                }
            } else {
                return "Upload failed! Response Code: " + response.statusCode() + ", Body: " + response.body();
            }
        } catch (ConnectException e) {
            return "Error: File upload service is not running. Please start the file-upload service first.";
        } catch (IOException | InterruptedException e) {
            return "Error: " + e.getMessage();
        }
    }
}
