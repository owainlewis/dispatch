package com.dispatch.monitoring;

import com.dispatch.core.filter.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HealthCheckFilter implements GatewayFilter {
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckFilter.class);
    
    private final ObjectMapper objectMapper;
    private final HealthChecker healthChecker;
    private final Instant startTime;
    
    public HealthCheckFilter() {
        this.objectMapper = new ObjectMapper();
        this.healthChecker = new DefaultHealthChecker();
        this.startTime = Instant.now();
    }
    
    public HealthCheckFilter(HealthChecker healthChecker) {
        this.objectMapper = new ObjectMapper();
        this.healthChecker = healthChecker;
        this.startTime = Instant.now();
    }
    
    @Override
    public String getName() {
        return "health-check";
    }
    
    @Override
    public boolean shouldApply(HttpRequest request) {
        String path = request.path();
        return "/health".equals(path) || "/health/ready".equals(path) || "/health/live".equals(path);
    }
    
    @Override
    public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String path = request.path();
                HealthStatus status = switch (path) {
                    case "/health" -> healthChecker.checkOverallHealth();
                    case "/health/ready" -> healthChecker.checkReadiness();
                    case "/health/live" -> healthChecker.checkLiveness();
                    default -> HealthStatus.unknown("Unknown health endpoint");
                };
                
                Map<String, Object> response = Map.of(
                    "status", status.getStatus().name(),
                    "timestamp", Instant.now().toString(),
                    "uptime", java.time.Duration.between(startTime, Instant.now()).toString(),
                    "checks", status.getChecks()
                );
                
                if (status.getDetails() != null && !status.getDetails().isEmpty()) {
                    response = new java.util.HashMap<>(response);
                    response.put("details", status.getDetails());
                }
                
                String jsonResponse = objectMapper.writeValueAsString(response);
                
                int statusCode = status.getStatus() == HealthStatus.Status.UP ? 200 : 503;
                HttpResponse httpResponse = new HttpResponse(statusCode, jsonResponse);
                httpResponse.setHeader("Content-Type", "application/json");
                
                logger.debug("Health check {} returned status: {}", path, status.getStatus());
                
                return FilterResult.respond(httpResponse);
                
            } catch (Exception e) {
                logger.error("Health check failed", e);
                return FilterResult.error(500, "Health check error");
            }
        });
    }
}