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
import java.util.Base64;

public class FileUploadService {
    private static final String UPLOAD_URL = "http://localhost:8081/api/files/upload";

    public static String uploadFile(File file) {
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
            
            return response.body();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
