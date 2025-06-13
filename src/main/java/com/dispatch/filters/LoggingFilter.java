package com.dispatch.filters;

import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LoggingFilter implements GatewayFilter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    
    private final String logLevel;
    private final boolean includeBody;
    private final boolean includeHeaders;
    
    public LoggingFilter() {
        this("INFO", false, false);
    }
    
    public LoggingFilter(DispatchConfig.FilterConfig config) {
        this.logLevel = config.getConfigString("level", "INFO");
        this.includeBody = config.getConfigBoolean("include-body", false);
        this.includeHeaders = config.getConfigBoolean("include-headers", false);
    }
    
    public LoggingFilter(String logLevel, boolean includeBody, boolean includeHeaders) {
        this.logLevel = logLevel;
        this.includeBody = includeBody;
        this.includeHeaders = includeHeaders;
    }
    
    @Override
    public String getName() {
        return "logging";
    }
    
    @Override
    public boolean shouldApply(HttpRequest request) {
        return true;
    }
    
    @Override
    public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            logRequest(request, context);
            return FilterResult.proceed();
        });
    }
    
    @Override
    public CompletableFuture<FilterResult> processResponse(HttpResponse response, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            logResponse(response, context);
            return FilterResult.proceed();
        });
    }
    
    private void logRequest(HttpRequest request, FilterContext context) {
        String clientIp = request.getClientIp();
        String method = request.method().name();
        String path = request.path();
        String userAgent = request.getHeader("User-Agent");
        
        StringBuilder logMessage = new StringBuilder()
            .append("REQUEST ")
            .append(clientIp).append(" ")
            .append(method).append(" ")
            .append(path);
        
        if (userAgent != null) {
            logMessage.append(" UA=").append(userAgent);
        }
        
        if (includeHeaders && request.headers() != null) {
            logMessage.append(" Headers={");
            request.headers().forEach(entry -> 
                logMessage.append(entry.getKey()).append("=").append(entry.getValue()).append(", ")
            );
            logMessage.append("}");
        }
        
        if (includeBody && request.body().length > 0) {
            String body = request.bodyAsString();
            if (body.length() > 1000) {
                body = body.substring(0, 1000) + "... (truncated)";
            }
            logMessage.append(" Body=").append(body);
        }
        
        logAtLevel(logMessage.toString());
    }
    
    private void logResponse(HttpResponse response, FilterContext context) {
        HttpRequest originalRequest = context.getOriginalRequest();
        Duration duration = Duration.between(context.getStartTime(), Instant.now());
        
        String clientIp = originalRequest.getClientIp();
        String method = originalRequest.method().name();
        String path = originalRequest.path();
        int status = response.statusCode();
        long durationMs = duration.toMillis();
        
        StringBuilder logMessage = new StringBuilder()
            .append("RESPONSE ")
            .append(clientIp).append(" ")
            .append(method).append(" ")
            .append(path).append(" ")
            .append(status).append(" ")
            .append(durationMs).append("ms");
        
        if (includeHeaders && response.headers() != null) {
            logMessage.append(" Headers={");
            response.headers().forEach(entry -> 
                logMessage.append(entry.getKey()).append("=").append(entry.getValue()).append(", ")
            );
            logMessage.append("}");
        }
        
        if (includeBody && response.body().length > 0) {
            String body = response.bodyAsString();
            if (body.length() > 1000) {
                body = body.substring(0, 1000) + "... (truncated)";
            }
            logMessage.append(" Body=").append(body);
        }
        
        logAtLevel(logMessage.toString());
    }
    
    private void logAtLevel(String message) {
        switch (logLevel.toUpperCase()) {
            case "DEBUG" -> logger.debug(message);
            case "INFO" -> logger.info(message);
            case "WARN" -> logger.warn(message);
            case "ERROR" -> logger.error(message);
            default -> logger.info(message);
        }
    }
}