package com.mpjmp.orchestrator.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "bulk_operation_status")
public class BulkOperationStatus {
    @Id
    private String id;
    private String status;
    private String errorMessage;
    // Add more fields as needed
}
