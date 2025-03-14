package com.mpjmp.storage.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/replicate")
public class ReplicationController {

    @Value("${file.storage-dir}")
    private String STORAGE_DIR;

    @PostMapping
    public String receiveFile(@RequestBody byte[] fileData, @RequestParam String fileName) {
        try {
            Files.write(Paths.get(STORAGE_DIR + fileName), fileData);
            return "File replicated successfully!";
        } catch (IOException e) {
            return "Replication failed: " + e.getMessage();
        }
    }
}
