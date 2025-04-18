import org.springframework.stereotype.Component;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class AtlasHealthIndicator implements HealthIndicator {
    private final MongoTemplate mongoTemplate;
    
    @Autowired
    public AtlasHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    @Override
    public Health health() {
        try {
            Document stats = mongoTemplate.executeCommand("{ serverStatus: 1 }");
            return Health.up()
                .withDetail("connections", stats.get("connections"))
                .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
