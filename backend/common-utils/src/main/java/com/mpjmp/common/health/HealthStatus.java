package com.mpjmp.common.health;

import java.util.Map;

public class HealthStatus {
    private final boolean healthy;
    private final Map<String, Boolean> details;

    public HealthStatus(boolean healthy, Map<String, Boolean> details) {
        this.healthy = healthy;
        this.details = details;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public Map<String, Boolean> getDetails() {
        return details;
    }
}
