package com.mpjmp.gui.api;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

public class AtlasUploadService {
    private final GridFSBucket gridFSBucket;

    public AtlasUploadService(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("file-orchestrator");
        this.gridFSBucket = GridFSBuckets.create(database);
    }

    public String uploadDirectToAtlas(File file) {
        try (FileInputStream streamToUploadFrom = new FileInputStream(file)) {
            ObjectId fileId = gridFSBucket.uploadFromStream(
                file.getName(), 
                streamToUploadFrom,
                new GridFSUploadOptions().metadata(new Document("contentType", Files.probeContentType(file.toPath())))
            );
            return fileId.toString();
        } catch (Exception e) {
            throw new RuntimeException("Direct upload failed", e);
        }
    }
}
