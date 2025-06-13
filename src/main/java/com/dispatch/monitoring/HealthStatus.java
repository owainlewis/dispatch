package com.dispatch.monitoring;

import java.util.Map;

public class HealthStatus {
    
    public enum Status {
        UP, DOWN, UNKNOWN
    }
    
    private final Status status;
    private final Map<String, Object> checks;
    private final Map<String, Object> details;
    
    public HealthStatus(Status status, Map<String, Object> checks, Map<String, Object> details) {
        this.status = status;
        this.checks = checks;
        this.details = details;
    }
    
    public static HealthStatus up(Map<String, Object> checks) {
        return new HealthStatus(Status.UP, checks, Map.of());
    }
    
    public static HealthStatus up() {
        return new HealthStatus(Status.UP, Map.of(), Map.of());
    }
    
    public static HealthStatus down(Map<String, Object> checks, Map<String, Object> details) {
        return new HealthStatus(Status.DOWN, checks, details);
    }
    
    public static HealthStatus down(String reason) {
        return new HealthStatus(Status.DOWN, Map.of(), Map.of("reason", reason));
    }
    
    public static HealthStatus unknown(String reason) {
        return new HealthStatus(Status.UNKNOWN, Map.of(), Map.of("reason", reason));
    }
    
    public Status getStatus() {
        return status;
    }
    
    public Map<String, Object> getChecks() {
        return checks;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public boolean isHealthy() {
        return status == Status.UP;
    }
}