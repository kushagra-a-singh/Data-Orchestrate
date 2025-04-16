package com.dataorchestrate.processing.kafka;

import com.dataorchestrate.processing.service.FileProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProcessingRequestConsumer {

    private final FileProcessingService fileProcessingService;

    @Autowired
    public ProcessingRequestConsumer(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @KafkaListener(
        topics = "${app.kafka.topics.processing.request}",
        groupId = "${app.kafka.groups.processing}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 5000)
    )
    public void handleProcessingRequest(
        String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
        @Header(KafkaHeaders.OFFSET) Long offset
    ) {
        log.info("Received processing request from topic: {}, partition: {}, offset: {}, message: {}", 
            topic, partition, offset, message);
        fileProcessingService.processFile(message);
    }
}