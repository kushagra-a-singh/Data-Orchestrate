package com.mpjmp.fileupload.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReplicationProducer {
    @Autowired
    private KafkaTemplate<String, ReplicationEvent> replicationEventKafkaTemplate;

    public void sendReplicationEvent(String topic, ReplicationEvent event) {
        replicationEventKafkaTemplate.send(topic, event);
    }
}
