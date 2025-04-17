package com.mpjmp.fileupload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import com.dataorchestrate.common.DeviceRepository;
import com.dataorchestrate.common.SequenceGenerator;
import com.mpjmp.fileupload.repository.CommonDeviceRepositoryImpl;

@Configuration
@ComponentScan(basePackages = "com.dataorchestrate.common")
public class CommonUtilsConfig {
    
    @Bean
    @Primary
    public DeviceRepository commonDeviceRepository(MongoTemplate mongoTemplate) {
        return new CommonDeviceRepositoryImpl(mongoTemplate);
    }
    
    @Bean
    public SequenceGenerator sequenceGenerator() {
        return new SequenceGenerator();
    }
}
