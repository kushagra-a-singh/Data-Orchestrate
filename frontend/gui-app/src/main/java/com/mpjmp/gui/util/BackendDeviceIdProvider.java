package com.mpjmp.gui.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class BackendDeviceIdProvider {
    private static String backendDeviceId = null;
    private static final String DEVICE_ID_URL = "http://localhost:8080/api/files/device/id";

    public static String getBackendDeviceId() {
        if (backendDeviceId != null) return backendDeviceId;
        try {
            URL url = new URL(DEVICE_ID_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int status = conn.getResponseCode();
            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
                in.close();
                JSONObject obj = new JSONObject(content.toString());
                backendDeviceId = obj.optString("deviceId", null);
                if (backendDeviceId == null || backendDeviceId.isEmpty()) {
                    backendDeviceId = null;
                }
            }
        } catch (Exception e) {
            // Could not fetch from backend
            backendDeviceId = null;
        }
        return backendDeviceId;
    }
}
