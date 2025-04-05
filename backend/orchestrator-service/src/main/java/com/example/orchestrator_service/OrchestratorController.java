package com.example.orchestrator_service;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orchestrate")
public class OrchestratorController {

    @PostMapping("/replicate/{fileId}")
    public String replicateFile(@PathVariable String fileId, @RequestBody String fileContent) {
        // Save the replicated file
        // Implement file saving logic here
        return "File replicated successfully: " + fileId;
    }
} 