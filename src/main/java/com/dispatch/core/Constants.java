package com.dispatch.core;

/**
 * Application-wide constants and default values
 */
public final class Constants {
    
    // Server Configuration
    public static final int DEFAULT_SERVER_PORT = 8080;
    public static final int DEFAULT_MAX_REQUEST_SIZE = 1_048_576; // 1MB
    public static final int DEFAULT_BACKLOG_SIZE = 1024;
    
    // Timeout Configuration
    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;  // 5 seconds
    public static final int DEFAULT_REQUEST_TIMEOUT_MS = 30000; // 30 seconds
    public static final int DEFAULT_MAX_RETRIES = 3;
    
    // Retry Configuration
    public static final long MIN_RETRY_DELAY_MS = 100;
    public static final long MAX_RETRY_DELAY_MS = 5000;
    public static final double RETRY_BACKOFF_MULTIPLIER = 2.0;
    
    // Health Check Configuration
    public static final String HEALTH_PATH = "/health";
    public static final String HEALTH_READY_PATH = "/health/ready";
    public static final String HEALTH_LIVE_PATH = "/health/live";
    
    // HTTP Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";
    public static final String HOST_HEADER = "Host";
    public static final String X_REQUEST_ID_HEADER = "X-Request-ID";
    public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    
    // Content Types
    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_HTML = "text/html";
    
    // Authentication
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String API_KEY_HEADER = "X-API-Key";
    
    // Rate Limiting
    public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 1000;
    public static final int DEFAULT_BURST_CAPACITY = 100;
    
    // Error Messages
    public static final String ERROR_NO_ROUTE_FOUND = "No matching route found";
    public static final String ERROR_NO_BACKEND_CONFIGURED = "No backend configured";
    public static final String ERROR_BACKEND_UNAVAILABLE = "Backend service unavailable";
    public static final String ERROR_AUTHENTICATION_FAILED = "Authentication failed";
    public static final String ERROR_RATE_LIMIT_EXCEEDED = "Rate limit exceeded";
    public static final String ERROR_INVALID_REQUEST = "Invalid request";
    
    // Circuit Breaker
    public static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5;
    public static final long CIRCUIT_BREAKER_TIMEOUT_MS = 60000; // 1 minute
    public static final long CIRCUIT_BREAKER_RETRY_TIMEOUT_MS = 10000; // 10 seconds
    
    private Constants() {
        // Utility class - prevent instantiation
    }
}