package com.example.storage_service.config;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.MongoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.web.client.RestTemplate;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MongoConfig {
    @Bean
    public GridFSBucket gridFsBucket(MongoDatabaseFactory mongoDatabaseFactory) {
        MongoDatabase db = mongoDatabaseFactory.getMongoDatabase();
        return GridFSBuckets.create(db);
    }

    @Bean
    public Path baseDirectory(@Value("${storage.dir:./data/storage}") String baseDir) {
        return Paths.get(baseDir);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
