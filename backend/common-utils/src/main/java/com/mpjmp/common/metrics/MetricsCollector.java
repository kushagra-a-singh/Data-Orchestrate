import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Collections;
import org.bson.Document;
import org.apache.kafka.common.TopicPartition;
import java.util.Set;

@Component
public class MetricsCollector {
    private final MeterRegistry meterRegistry;
    private final MongoTemplate mongoTemplate;
    private final KafkaConsumer<String, String> kafkaConsumer;
    
    @Autowired
    public MetricsCollector(MeterRegistry meterRegistry, MongoTemplate mongoTemplate, KafkaConsumer<String, String> kafkaConsumer) {
        this.meterRegistry = meterRegistry;
        this.mongoTemplate = mongoTemplate;
        this.kafkaConsumer = kafkaConsumer;
    }

    @Scheduled(fixedRate = 5000)
    public void collect() {
        // Atlas metrics
        Document serverStatus = mongoTemplate.executeCommand("{serverStatus: 1}");
        Document connections = serverStatus.get("connections", Document.class);
        int currentConnections = connections != null ? connections.getInteger("current", 0) : 0;
        meterRegistry.gauge("mongodb.connections", currentConnections);

        // Kafka metrics
        Set<TopicPartition> topicPartitions = kafkaConsumer.assignment();
        long lag = 0;
        if (!topicPartitions.isEmpty()) {
            lag = kafkaConsumer.endOffsets(topicPartitions)
                    .values().stream().mapToLong(v -> v).sum();
        }
        meterRegistry.gauge("kafka.lag", lag);
    }
}
