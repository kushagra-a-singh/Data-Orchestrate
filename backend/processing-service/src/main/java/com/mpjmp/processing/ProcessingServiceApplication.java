package com.mpjmp.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableKafka
@EnableRetry
public class ProcessingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProcessingServiceApplication.class, args);
    }
} 