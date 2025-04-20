package com.mpjmp.gui.api;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataorchestrate.common.DeviceConfigUtil;

public class FileDownloadService {
    // Use devices.json for all device info
    private static List<Map<String, String>> allDevices;
    private static String selfDeviceName;
    private static String downloadUrlPrefix;
    static {
        try {
            // Use backend DeviceConfigUtil for device info
            allDevices = DeviceConfigUtil.getAllDevices();
            selfDeviceName = DeviceConfigUtil.getSelfDeviceName();
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(selfDeviceName)).findFirst().orElse(null);
            if (self != null) {
                downloadUrlPrefix = "http://" + self.get("ip") + ":" + self.get("port") + "/api/files/download/";
            } else {
                downloadUrlPrefix = null;
            }
        } catch (Exception e) {
            selfDeviceName = "UNKNOWN";
            downloadUrlPrefix = null;
        }
    }

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static String downloadFile(String fileId, String savePath) {
        try {
            HttpResponse<byte[]> response = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrlPrefix + fileId))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() == 200) {
                Files.write(Paths.get(savePath), response.body());
                
                // Remove metadata fetch (not needed for HTTP orchestration)
                return "Download successful.";
            }
            return "Download failed! Response Code: " + response.statusCode();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
