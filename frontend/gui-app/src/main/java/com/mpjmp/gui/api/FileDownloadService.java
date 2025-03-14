package com.mpjmp.gui.api;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileDownloadService {
    private static final String DOWNLOAD_URL = "http://localhost:8085/download/";

    public static String downloadFile(String fileId, String savePath) {
        try {
            URL url = new URL(DOWNLOAD_URL + fileId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                try (InputStream inputStream = connection.getInputStream()) {
                    Files.copy(inputStream, Paths.get(savePath));
                }
                return "Download successful: " + savePath;
            } else {
                return "Download failed! Response Code: " + connection.getResponseCode();
            }
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }
}
