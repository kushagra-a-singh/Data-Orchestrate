package com.mpjmp.fileupload.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class MongoConfig {
    @Bean
    public GridFSBucket gridFSBucket(MongoClient mongoClient, @Value("${spring.data.mongodb.database}") String dbName) {
        MongoDatabase db = mongoClient.getDatabase(dbName);
        return GridFSBuckets.create(db);
    }
}
