package com.mpjmp.processing.kafka;

import com.mpjmp.processing.service.FileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessingRequestConsumer {

    private final FileProcessingService fileProcessingService;

    @KafkaListener(
        topics = "${kafka.topic.processing-request}",
        groupId = "${spring.kafka.consumer.group-id}",
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