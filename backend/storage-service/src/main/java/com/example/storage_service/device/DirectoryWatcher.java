package com.example.storage_service.device;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;

@Component
public class DirectoryWatcher {
    private final WatchService watchService;

    public DirectoryWatcher() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
    }

    @PostConstruct
    public void init() throws IOException {
        Path dir = Paths.get(System.getProperty("user.home"), "sync_dir");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
        new Thread(this::watch).start();
    }

    private void watch() {
        while (true) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    // Handle file change event (sync logic here)
                }
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
