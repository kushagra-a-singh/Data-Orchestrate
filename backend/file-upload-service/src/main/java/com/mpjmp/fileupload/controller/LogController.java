package com.mpjmp.fileupload.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Collections;

@RestController
public class LogController {
    private static final String LOG_PATH = "logs/app.log";

    @GetMapping("/api/logs/latest")
    public List<String> getLatestLogs() {
        try {
            List<String> allLines = Files.readAllLines(Paths.get(LOG_PATH));
            int fromIndex = Math.max(allLines.size() - 100, 0);
            return allLines.subList(fromIndex, allLines.size());
        } catch (Exception e) {
            return Collections.singletonList("Error reading logs: " + e.getMessage());
        }
    }
}
