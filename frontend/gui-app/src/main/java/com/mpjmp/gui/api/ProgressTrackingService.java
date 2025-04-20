package com.mpjmp.gui.api;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.net.ConnectException;
import java.io.InputStream;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dataorchestrate.common.DeviceConfigUtil;

public class ProgressTrackingService {
    // Load device info from a config file (devices.json)
    private static List<Map<String, String>> allDevices;
    private static String selfDeviceName;
    private static String progressUrl;
    static {
        try {
            // Use backend DeviceConfigUtil for device info
            allDevices = DeviceConfigUtil.getAllDevices();
            selfDeviceName = DeviceConfigUtil.getSelfDeviceName();
            Map<String, String> self = allDevices.stream().filter(d -> d.get("name").equals(selfDeviceName)).findFirst().orElse(null);
            if (self != null) {
                progressUrl = "http://" + self.get("ip") + ":" + self.get("port") + "/progress/";
            }
        } catch (Exception e) {
            selfDeviceName = "UNKNOWN";
            throw new RuntimeException("Failed to load device config", e);
        }
    }

    public static String getProcessingProgress(String fileName) {
        try {
            // Extract file ID from the file name if needed
            String fileId = fileName;
            
            URL url = new URL(progressUrl + fileId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds timeout

            if (connection.getResponseCode() == 200) {
                Scanner scanner = new Scanner(connection.getInputStream()).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "0.0";
            } else {
                // If service is not available, return a default progress
                return "0.5"; // Return 50% progress as a default
            }
        } catch (ConnectException e) {
            // If the processing service is not running, return a default progress
            return "0.5"; // Return 50% progress as a default
        } catch (Exception e) {
            // For other errors, return a default progress
            return "0.5"; // Return 50% progress as a default
        }
    }
}
