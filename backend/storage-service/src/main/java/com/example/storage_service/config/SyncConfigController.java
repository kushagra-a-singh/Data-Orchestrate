package com.example.storage_service.config;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.example.storage_service.repository.SyncRuleRepository;
import com.example.storage_service.model.SyncRule;

@RestController
@RequestMapping("/api/sync-config")
@RequiredArgsConstructor
public class SyncConfigController {
    private final SyncRuleRepository ruleRepository;

    @GetMapping("/{deviceId}")
    public List<SyncRule> getRules(@PathVariable String deviceId) {
        return ruleRepository.findByDeviceId(deviceId);
    }

    @PostMapping
    public SyncRule saveRule(@RequestBody SyncRule rule) {
        return ruleRepository.save(rule);
    }

    @DeleteMapping("/{id}")
    public void deleteRule(@PathVariable String id) {
        ruleRepository.deleteById(id);
    }
}
