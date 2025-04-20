package com.mpjmp.fileupload.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import com.mpjmp.fileupload.DotenvInitializer;

@SpringBootTest
@ContextConfiguration(initializers = DotenvInitializer.class)
public class FileUploadServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
// Removed TestConfig import as the file is deleted and no longer needed
