package com.example.storage_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.TestPropertySource;
import com.example.storage_service.repository.FileDistributionRepository;
import com.example.storage_service.repository.DeviceRepository;
import com.example.storage_service.repository.LocalFileRegistryRepository;
import org.mockito.Mockito;
import java.util.concurrent.CompletableFuture;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/testdb",
    "spring.data.mongodb.database=testdb",
    "server.port=8084",
    "file.storage-dir=target/test-storage",
    "device.id=test-device-1",
    "net.bytebuddy.experimental=true"
})
class StorageServiceApplicationTests {

	@Configuration
	static class TestConfig {
		@Bean
		@Primary
		public KafkaTemplate<String, String> kafkaTemplate() {
			// Create a simple KafkaTemplate implementation for testing
			return new KafkaTemplate<String, String>(Mockito.mock(ProducerFactory.class)) {
				@Override
				public CompletableFuture<SendResult<String, String>> send(String topic, String data) {
					return CompletableFuture.completedFuture(new SendResult<>(null, null));
				}
			};
		}
		
		@Bean
		@Primary
		public FileDistributionRepository fileDistributionRepository() {
			return Mockito.mock(FileDistributionRepository.class);
		}
		
		@Bean
		@Primary
		public DeviceRepository deviceRepository() {
			return Mockito.mock(DeviceRepository.class);
		}
		
		@Bean
		@Primary
		public LocalFileRegistryRepository localFileRegistryRepository() {
			return Mockito.mock(LocalFileRegistryRepository.class);
		}
		
		@Bean
		@Primary
		public FileDistributionService fileDistributionService(
				FileDistributionRepository distributionRepository,
				DeviceRepository deviceRepository,
				LocalFileRegistryRepository localFileRegistry) {
			return new FileDistributionService(
				distributionRepository,
				deviceRepository,
				localFileRegistry
			);
		}
	}

	@Test
	void contextLoads() {
	}

}
