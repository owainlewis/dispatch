package com.dispatch.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.util.Map;

public class DefaultHealthChecker implements HealthChecker {
    private static final Logger logger = LoggerFactory.getLogger(DefaultHealthChecker.class);
    
    private static final long MEMORY_THRESHOLD_PERCENT = 90;
    
    @Override
    public HealthStatus checkOverallHealth() {
        try {
            Map<String, Object> checks = Map.of(
                "memory", checkMemoryUsage(),
                "system", checkSystemHealth(),
                "timestamp", Instant.now().toString()
            );
            
            boolean allHealthy = checks.values().stream()
                .filter(v -> v instanceof Map)
                .map(v -> (Map<?, ?>) v)
                .allMatch(check -> "UP".equals(check.get("status")));
            
            return allHealthy ? HealthStatus.up(checks) : HealthStatus.down(checks, Map.of());
            
        } catch (Exception e) {
            logger.error("Overall health check failed", e);
            return HealthStatus.down("Health check error: " + e.getMessage());
        }
    }
    
    @Override
    public HealthStatus checkReadiness() {
        try {
            Map<String, Object> checks = Map.of(
                "memory", checkMemoryUsage(),
                "ready", Map.of("status", "UP", "message", "Service is ready to accept requests")
            );
            
            return HealthStatus.up(checks);
            
        } catch (Exception e) {
            logger.error("Readiness check failed", e);
            return HealthStatus.down("Readiness check error: " + e.getMessage());
        }
    }
    
    @Override
    public HealthStatus checkLiveness() {
        try {
            Map<String, Object> checks = Map.of(
                "live", Map.of("status", "UP", "message", "Service is alive"),
                "timestamp", Instant.now().toString()
            );
            
            return HealthStatus.up(checks);
            
        } catch (Exception e) {
            logger.error("Liveness check failed", e);
            return HealthStatus.down("Liveness check error: " + e.getMessage());
        }
    }
    
    private Map<String, Object> checkMemoryUsage() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            
            double usagePercent = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0;
            
            String status = usagePercent < MEMORY_THRESHOLD_PERCENT ? "UP" : "DOWN";
            String message = String.format("Memory usage: %.1f%% (%d/%d bytes)", 
                usagePercent, usedMemory, maxMemory);
            
            return Map.of(
                "status", status,
                "message", message,
                "usedBytes", usedMemory,
                "maxBytes", maxMemory,
                "usagePercent", Math.round(usagePercent * 100.0) / 100.0
            );
            
        } catch (Exception e) {
            logger.warn("Memory check failed", e);
            return Map.of(
                "status", "UNKNOWN",
                "message", "Unable to check memory usage: " + e.getMessage()
            );
        }
    }
    
    private Map<String, Object> checkSystemHealth() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            double systemLoadAverage = osBean.getSystemLoadAverage();
            int availableProcessors = osBean.getAvailableProcessors();
            
            String loadStatus = systemLoadAverage < 0 ? "UNKNOWN" : 
                (systemLoadAverage < availableProcessors * 2 ? "UP" : "DOWN");
            
            String message = systemLoadAverage < 0 ? 
                "System load average not available" :
                String.format("System load: %.2f (processors: %d)", systemLoadAverage, availableProcessors);
            
            return Map.of(
                "status", loadStatus,
                "message", message,
                "systemLoadAverage", systemLoadAverage,
                "availableProcessors", availableProcessors
            );
            
        } catch (Exception e) {
            logger.warn("System check failed", e);
            return Map.of(
                "status", "UNKNOWN",
                "message", "Unable to check system health: " + e.getMessage()
            );
        }
    }
}