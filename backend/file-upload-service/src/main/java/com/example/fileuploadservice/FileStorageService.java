package com.mpjmp.fileupload.service;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FileStorageService {

    private final GridFSBucket gridFsBucket;

    @Autowired
    public FileStorageService(GridFSBucket gridFsBucket) {
        this.gridFsBucket = gridFsBucket;
    }

    // --- METADATA VALIDATION ---
    private void validateMetadata(MultipartFile file, String uploadedBy, String targetDirectory) {
        if (uploadedBy == null || uploadedBy.isBlank()) throw new IllegalArgumentException("Uploader must be specified");
        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) throw new IllegalArgumentException("Filename required");
        if (file.getContentType() == null || file.getContentType().isBlank()) throw new IllegalArgumentException("File type required");
        if (targetDirectory == null) throw new IllegalArgumentException("Target directory required");
    }

    // --- ENSURE FULL METADATA ON UPLOAD ---
    public String storeFile(MultipartFile file, String uploadedBy, String targetDirectory) throws IOException {
        validateMetadata(file, uploadedBy, targetDirectory);
        Document metadata = new Document()
            .append("uploadedBy", uploadedBy)
            .append("originalFilename", file.getOriginalFilename())
            .append("contentType", file.getContentType())
            .append("targetDirectory", targetDirectory)
            .append("timestamp", System.currentTimeMillis());
        ObjectId fileId = gridFsBucket.uploadFromStream(
            file.getOriginalFilename(), 
            file.getInputStream(),
            new GridFSUploadOptions().metadata(metadata)
        );
        return fileId.toString();
    }
    
    public byte[] getFile(String fileId) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gridFsBucket.downloadToStream(new ObjectId(fileId), outputStream);
        return outputStream.toByteArray();
    }
}
