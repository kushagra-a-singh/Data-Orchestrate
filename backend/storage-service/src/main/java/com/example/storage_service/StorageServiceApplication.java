package com.mpjmp.storage;

import com.mpjmp.storage.service.DeviceRegistryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StorageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner run(DeviceRegistryService deviceRegistryService) {
        return args -> deviceRegistryService.registerDevice();
    }
}
