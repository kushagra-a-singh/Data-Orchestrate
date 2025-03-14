package com.mpjmp.processing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ProcessingService {

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Value("${file.processed-dir}")
    private String PROCESSED_DIR;

    public void processFile(String fileId, String filePath, String operationType) {
        executorService.submit(() -> {
            try {
                String basePath = Paths.get("").toAbsolutePath().toString(); // Get project root
                String processedFilePath = Paths.get(basePath, PROCESSED_DIR, fileId + "-processed").toString();

                Files.createDirectories(Paths.get(basePath, PROCESSED_DIR)); // Ensure directory exists
                
                // Simulating Processing
                System.out.println("Processing " + filePath + " with " + operationType);
                Files.copy(Paths.get(filePath), Paths.get(processedFilePath)); // Copy file after processing

                System.out.println("Processed file saved at: " + processedFilePath);
            } catch (Exception e) {
                System.err.println("Error processing file: " + e.getMessage());
            }
        });
    }
}
