package com.mpjmp.common.health;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.mpjmp.common.health.HealthStatus;

@RestController
@RequestMapping("/health")
public class HealthController {
    
    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    public HealthController(MongoTemplate mongoTemplate, KafkaTemplate<String, String> kafkaTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @GetMapping
    public ResponseEntity<HealthStatus> checkHealth() {
        boolean mongoHealthy = checkMongo();
        boolean kafkaHealthy = checkKafka();
        
        HealthStatus status = new HealthStatus(
            mongoHealthy && kafkaHealthy,
            Map.of(
                "mongodb", mongoHealthy,
                "kafka", kafkaHealthy
            )
        );
        
        return status.isHealthy() ? 
            ResponseEntity.ok(status) : 
            ResponseEntity.status(503).body(status);
    }
    
    private boolean checkMongo() {
        try {
            mongoTemplate.executeCommand("{ping:1}");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkKafka() {
        try {
            return kafkaTemplate.send("health-check", "test").get(1, TimeUnit.SECONDS) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
