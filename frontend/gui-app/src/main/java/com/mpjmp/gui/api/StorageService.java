package com.mpjmp.gui.api;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

public class StorageService {
    private static final String STORAGE_SERVICE_URL = "http://localhost:8085";
    private static final RestTemplate restTemplate;

    static {
        restTemplate = new RestTemplate();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_OCTET_STREAM
        ));
        restTemplate.getMessageConverters().add(0, converter);
    }

    public static String registerFile(String fileId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("fileId", fileId);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            STORAGE_SERVICE_URL + "/api/storage/register",
            request,
            String.class
        );

        return response.getBody();
    }
} 