package com.example.fileuploadservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {
    private String dir;
    private long maxFileSize;
    private List<String> allowedTypes;
} 