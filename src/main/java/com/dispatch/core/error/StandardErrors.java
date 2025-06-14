package com.dispatch.core.error;

import com.dispatch.core.Constants;
import com.dispatch.core.filter.FilterResult;
import com.dispatch.core.filter.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Factory for creating standardized error responses
 */
public final class StandardErrors {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static FilterResult noRouteFound(String requestId) {
        return createErrorResult(404, Constants.ERROR_NO_ROUTE_FOUND, null, requestId);
    }
    
    public static FilterResult noBackendConfigured(String requestId) {
        return createErrorResult(503, Constants.ERROR_NO_BACKEND_CONFIGURED, null, requestId);
    }
    
    public static FilterResult backendUnavailable(String requestId) {
        return createErrorResult(502, Constants.ERROR_BACKEND_UNAVAILABLE, null, requestId);
    }
    
    public static FilterResult authenticationFailed(String requestId) {
        return createErrorResult(401, Constants.ERROR_AUTHENTICATION_FAILED, null, requestId);
    }
    
    public static FilterResult rateLimitExceeded(String requestId) {
        return createErrorResult(429, Constants.ERROR_RATE_LIMIT_EXCEEDED, null, requestId);
    }
    
    public static FilterResult invalidRequest(String details, String requestId) {
        return createErrorResult(400, Constants.ERROR_INVALID_REQUEST, details, requestId);
    }
    
    public static FilterResult internalServerError(String details, String requestId) {
        return createErrorResult(500, "Internal server error", details, requestId);
    }
    
    private static FilterResult createErrorResult(int statusCode, String message, String details, String requestId) {
        try {
            ErrorResponse errorResponse = ErrorResponse.create(statusCode, message, details, requestId);
            String jsonBody = objectMapper.writeValueAsString(errorResponse);
            
            HttpResponse response = new HttpResponse(statusCode, jsonBody);
            response.setHeader(Constants.CONTENT_TYPE_HEADER, Constants.APPLICATION_JSON);
            response.setHeader(Constants.X_REQUEST_ID_HEADER, errorResponse.requestId());
            
            return FilterResult.respond(response);
        } catch (Exception e) {
            // Fallback to simple error response if JSON serialization fails
            HttpResponse response = new HttpResponse(statusCode, message);
            response.setHeader(Constants.CONTENT_TYPE_HEADER, Constants.TEXT_PLAIN);
            return FilterResult.respond(response);
        }
    }
    
    private StandardErrors() {
        // Utility class - prevent instantiation
    }
}