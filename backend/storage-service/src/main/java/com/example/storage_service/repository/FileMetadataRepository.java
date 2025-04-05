package com.example.storage_service.repository;

import com.example.storage_service.model.FileMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {
} 