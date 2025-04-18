package com.example.storage_service.model;

import org.bson.Document;
import java.io.Serializable;

public class FileTransferPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    private final byte[] fileData;
    private final Document metadata;

    public FileTransferPayload(byte[] fileData, Document metadata) {
        this.fileData = fileData;
        this.metadata = metadata;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public Document getMetadata() {
        return metadata;
    }
}
