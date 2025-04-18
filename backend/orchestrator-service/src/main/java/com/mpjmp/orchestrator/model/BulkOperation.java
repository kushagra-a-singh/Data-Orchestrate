package com.mpjmp.orchestrator.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@Document(collection = "bulk_operations")
public class BulkOperation {
    @Id
    private String id;
    private List<String> fileIds;
    // Add more fields as needed
    public BulkOperation(String id, List<String> fileIds) {
        this.id = id;
        this.fileIds = fileIds;
    }
    public BulkOperation() {}
}
