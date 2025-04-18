package com.example.storage_service.events;

import java.util.List;

public record FileChangeEvent(String fileId, String filename, List<String> deviceIds) {
}
