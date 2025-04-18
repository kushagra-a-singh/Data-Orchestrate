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
            HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                    .uri(URI.create(DOWNLOAD_URL + fileId))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() == 200) {
                Files.write(Paths.get(savePath), response.body());
                
                // Get metadata
                HttpResponse<String> metaResponse = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                        .uri(URI.create(DOWNLOAD_URL + fileId + "/metadata"))
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                );
                
                return "Download successful. Metadata: " + metaResponse.body();
            }
            return "Download failed! Response Code: " + response.statusCode();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
