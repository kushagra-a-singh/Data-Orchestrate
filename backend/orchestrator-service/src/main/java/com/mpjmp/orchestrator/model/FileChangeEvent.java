package com.mpjmp.orchestrator.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "file_change_events")
public class FileChangeEvent {
    @Id
    private String id;
    private String fileId;
    private String filename;
    private List<String> deviceIds;

    public FileChangeEvent() {}

    public FileChangeEvent(String fileId, String filename) {
        this.fileId = fileId;
        this.filename = filename;
    }

    public String getFileId() { return fileId; }

    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFilename() { return filename; }

    public void setFilename(String filename) { this.filename = filename; }
}
