package com.mpjmp.gui.api;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileDownloadService {
    private static final String DOWNLOAD_URL = "http://localhost:8085/download/";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static String downloadFile(String fileId, String savePath) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DOWNLOAD_URL + fileId))
                .GET()
                .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                try (InputStream inputStream = response.body()) {
                    Files.copy(inputStream, Paths.get(savePath));
                }
                return "Download successful: " + savePath;
            } else {
                return "Download failed! Response Code: " + response.statusCode();
            }
        } catch (IOException | InterruptedException e) {
            return "Error: " + e.getMessage();
        }
    }
}
