package com.example.processing_service;

import com.example.common.repository.FileMetadataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/testdb",
    "spring.data.mongodb.database=testdb",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.group-id=test-group"
})
class ProcessingServiceApplicationTests {

	@MockBean
	private FileMetadataRepository fileMetadataRepository;

	@Test
	void contextLoads() {
	}

}
