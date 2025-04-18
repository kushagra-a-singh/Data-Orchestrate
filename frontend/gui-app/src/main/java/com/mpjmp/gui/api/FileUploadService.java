package com.mpjmp.gui.api;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.net.ConnectException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Base64;

public class FileUploadService {
    private static final String UPLOAD_URL = "http://localhost:8081/api/files/upload";

    public static String uploadFile(File file, String orchestratorUrl, String deviceListEndpoint) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("fileName", file.getName());
            payload.put("fileData", Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath())));
            
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                    .uri(URI.create(UPLOAD_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            
            // After upload, trigger replication to all devices
            String fileId = extractFileId(response.body());
            JSONArray devices = fetchDeviceList(deviceListEndpoint);
            for (int i = 0; i < devices.length(); i++) {
                JSONObject device = devices.getJSONObject(i);
                String deviceUrl = device.getString("url");
                JSONObject replicationRequest = new JSONObject();
                replicationRequest.put("fileId", fileId);
                replicationRequest.put("fileName", file.getName());
                replicationRequest.put("sourceDeviceUrl", orchestratorUrl);
                HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                        .uri(URI.create(deviceUrl + "/replicate"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(replicationRequest.toString()))
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                );
            }
            return response.body();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Helper to extract fileId from upload response
    private static String extractFileId(String responseBody) {
        // Implement extraction logic based on backend response structure
        return responseBody; // Placeholder
    }

    // Helper to fetch device list
    private static JSONArray fetchDeviceList(String deviceListEndpoint) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create(deviceListEndpoint))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        return new JSONArray(response.body());
    }
}
