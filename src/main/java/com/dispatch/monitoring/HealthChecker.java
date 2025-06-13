package com.dispatch.monitoring;

public interface HealthChecker {
    
    HealthStatus checkOverallHealth();
    
    HealthStatus checkReadiness();
    
    HealthStatus checkLiveness();
}