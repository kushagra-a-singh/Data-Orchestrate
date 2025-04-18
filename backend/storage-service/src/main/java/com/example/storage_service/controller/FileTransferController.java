package com.example.storage_service.controller;

import com.mongodb.client.gridfs.GridFSBucket;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileTransferController {
    @Autowired
    private final GridFSBucket gridFsBucket;

    // Download file by fileId
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileId) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gridFsBucket.downloadToStream(new ObjectId(fileId), outputStream);
        byte[] fileBytes = outputStream.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(fileBytes);
        InputStreamResource resource = new InputStreamResource(inputStream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileId)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
