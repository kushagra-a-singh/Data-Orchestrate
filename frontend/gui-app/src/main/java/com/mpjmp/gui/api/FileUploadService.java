package com.mpjmp.gui.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.net.ConnectException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataorchestrate.common.DeviceConfigUtil;

public class FileUploadService {
    // Use devices.json for all device info
    private static List<Map<String, String>> allDevices;
    private static String selfDeviceName;
    private static String uploadUrl;
    private static String replicateUrl;
    static {
        try {
            // Use backend DeviceConfigUtil for device info
            allDevices = DeviceConfigUtil.getAllDevices();
            selfDeviceName = DeviceConfigUtil.getSelfDeviceName();
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(selfDeviceName)).findFirst().orElse(null);
            if (self != null) {
                uploadUrl = "http://" + self.get("ip") + ":" + self.get("port") + "/api/files/upload";
                replicateUrl = "http://" + self.get("ip") + ":" + self.get("port") + "/api/files/replicate";
            } else {
                uploadUrl = null;
                replicateUrl = null;
            }
        } catch (Exception e) {
            uploadUrl = null;
            replicateUrl = null;
        }
    }

    public static String uploadFile(File file, String uploadedBy, String deviceId) {
        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            var byteArrays = new java.util.ArrayList<byte[]>();
            // File part
            String filePartHeader = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                    "Content-Type: application/pdf\r\n\r\n";
            byteArrays.add(filePartHeader.getBytes(StandardCharsets.UTF_8));
            byteArrays.add(Files.readAllBytes(file.toPath()));
            byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
            // uploadedBy part
            String uploadedByPart = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"uploadedBy\"\r\n\r\n" +
                    uploadedBy + "\r\n";
            byteArrays.add(uploadedByPart.getBytes(StandardCharsets.UTF_8));
            // deviceId part
            String deviceIdPart = "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"deviceId\"\r\n\r\n" +
                    deviceId + "\r\n";
            byteArrays.add(deviceIdPart.getBytes(StandardCharsets.UTF_8));
            // End boundary
            String endBoundary = "--" + boundary + "--\r\n";
            byteArrays.add(endBoundary.getBytes(StandardCharsets.UTF_8));
            // Combine all parts
            int totalLength = byteArrays.stream().mapToInt(b -> b.length).sum();
            byte[] multipartBody = new byte[totalLength];
            int pos = 0;
            for (byte[] b : byteArrays) {
                System.arraycopy(b, 0, multipartBody, pos, b.length);
                pos += b.length;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String replicateFile(String fileId, String fileName, String deviceId, String sourceDeviceUrl) {
        try {
            JSONObject replicationRequest = new JSONObject();
            replicationRequest.put("fileId", fileId);
            replicationRequest.put("fileName", fileName);
            replicationRequest.put("deviceId", deviceId);
            // Always send the ACTUAL deviceId used for upload as part of the URL for download
            // sourceDeviceUrl should NOT include /api/files/download, just the base URL
            // The backend expects to construct: <sourceDeviceUrl>/api/files/download/<deviceId>/<fileName>
            replicationRequest.put("sourceDeviceUrl", sourceDeviceUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(replicateUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(replicationRequest.toString()))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
