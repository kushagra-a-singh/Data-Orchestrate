package com.mpjmp.gui.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

public class FileUploadService {
    private static final String UPLOAD_URL = "http://localhost:8081/upload";

    public static String uploadFile(File file) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary");
            
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(Files.readAllBytes(file.toPath()));
            outputStream.close();
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return "Upload successful!";
            } else {
                return "Upload failed! Response Code: " + responseCode;
            }
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }
}
