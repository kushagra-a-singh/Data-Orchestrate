package com.mpjmp.fileupload.controller;

import com.mpjmp.fileupload.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileId = fileStorageService.storeFile(file);
        return ResponseEntity.ok("File uploaded successfully, File ID: " + fileId);
    }
}
