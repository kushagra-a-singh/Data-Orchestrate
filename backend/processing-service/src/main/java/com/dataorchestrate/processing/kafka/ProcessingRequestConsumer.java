package com.dataorchestrate.processing.kafka;

import com.dataorchestrate.processing.service.FileProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
public class ProcessingRequestConsumer {

    private final FileProcessingService fileProcessingService;
    private final RestTemplate restTemplate;

    @Autowired
    public ProcessingRequestConsumer(FileProcessingService fileProcessingService, RestTemplate restTemplate) {
        this.fileProcessingService = fileProcessingService;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/processing-requests")
    public ResponseEntity<String> handleProcessingRequest(@RequestBody String message) {
        log.info("Received processing request: {}", message);
        fileProcessingService.processFile(message);
        return ResponseEntity.accepted().build();
    }

    // Alternatively, you can use polling to fetch processing requests
    // @Scheduled(fixedDelay = 1000)
    // public void pollProcessingRequests() {
    //     String message = restTemplate.getForObject("https://example.com/processing-requests", String.class);
    //     if (message != null) {
    //         log.info("Received processing request: {}", message);
    //         fileProcessingService.processFile(message);
    //     }
    // }
}