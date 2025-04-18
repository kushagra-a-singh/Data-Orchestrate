package com.example.storage_service.model;

public class FileChunk {
    private final String fileId;
    private final byte[] data;

    public FileChunk(String fileId, byte[] data) {
        this.fileId = fileId;
        this.data = data;
    }

    public String getFileId() {
        return fileId;
    }

    public byte[] getData() {
        return data;
    }
}
