package com.mpjmp.orchestrator.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.mpjmp.orchestrator.model.ReplicationProgress;
import com.mpjmp.orchestrator.service.ReplicationTracker;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {
    private final ReplicationTracker tracker;
    
    @GetMapping("/{fileId}/{deviceId}")
    public ReplicationProgress getProgress(
        @PathVariable String fileId, 
        @PathVariable String deviceId
    ) {
        return tracker.getProgress(fileId, deviceId);
    }
}
