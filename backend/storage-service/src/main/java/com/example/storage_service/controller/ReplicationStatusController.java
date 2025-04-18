package com.example.storage_service.controller;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.client.gridfs.model.GridFSFile;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/replication-status")
@RequiredArgsConstructor
public class ReplicationStatusController {
    @Autowired
    private final MongoTemplate mongoTemplate;
    @Autowired
    private final GridFsTemplate gridFsTemplate;

    // Get all files replicated for a device
    @GetMapping("/device/{deviceId}")
    public List<Document> getReplicatedFilesForDevice(@PathVariable String deviceId) {
        Query query = new Query(Criteria.where("deviceId").is(deviceId));
        return mongoTemplate.find(query, Document.class, "replication_status");
    }

    // Get all devices that have replicated a file
    @GetMapping("/file/{fileId}")
    public List<Document> getDevicesForFile(@PathVariable String fileId) {
        Query query = new Query(Criteria.where("fileId").is(fileId));
        return mongoTemplate.find(query, Document.class, "replication_status");
    }

    // Get files pending replication for a device
    @GetMapping("/device/{deviceId}/pending")
    public List<Document> getPendingFilesForDevice(@PathVariable String deviceId) {
        // All files in GridFS
        List<GridFSFile> allFiles = new ArrayList<>();
        gridFsTemplate.find(new Query()).forEach(allFiles::add);
        // All replicated files for this device
        Query repQ = new Query(Criteria.where("deviceId").is(deviceId));
        List<String> replicatedFileIds = mongoTemplate.find(repQ, Document.class, "replication_status").stream()
            .map(doc -> doc.getString("fileId")).toList();
        // Filter
        List<Document> pending = new ArrayList<>();
        for (GridFSFile file : allFiles) {
            if (!replicatedFileIds.contains(file.getObjectId().toString())) {
                Document d = new Document("fileId", file.getObjectId().toString())
                    .append("filename", file.getFilename())
                    .append("metadata", file.getMetadata());
                pending.add(d);
            }
        }
        return pending;
    }
}
