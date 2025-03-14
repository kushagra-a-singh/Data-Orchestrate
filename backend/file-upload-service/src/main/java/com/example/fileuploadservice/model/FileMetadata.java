package com.mpjmp.fileupload.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "files")
public class FileMetadata {
    @Id
    private String id;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String status;
    private String storagePath;
    private long timestamp;

    public FileMetadata(String fileName, String fileType, long fileSize, String storagePath) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.status = "Pending";
        this.timestamp = System.currentTimeMillis();
    }
}
