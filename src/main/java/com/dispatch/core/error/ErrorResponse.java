package com.dispatch.core.error;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Standardized error response format for all API errors
 */
public record ErrorResponse(
    @JsonProperty("error") ErrorDetails error,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("requestId") String requestId
) {
    
    public static ErrorResponse create(int code, String message) {
        return create(code, message, null, null);
    }
    
    public static ErrorResponse create(int code, String message, String requestId) {
        return create(code, message, null, requestId);
    }
    
    public static ErrorResponse create(int code, String message, String details, String requestId) {
        return new ErrorResponse(
            new ErrorDetails(code, message, details),
            Instant.now().toString(),
            requestId != null ? requestId : UUID.randomUUID().toString()
        );
    }
    
    public record ErrorDetails(
        @JsonProperty("code") int code,
        @JsonProperty("message") String message,
        @JsonProperty("details") String details
    ) {}
}