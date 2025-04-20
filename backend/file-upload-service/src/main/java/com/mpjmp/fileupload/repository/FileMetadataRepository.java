package com.mpjmp.fileupload.repository;

import com.mpjmp.common.model.FileMetadata;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {
} 