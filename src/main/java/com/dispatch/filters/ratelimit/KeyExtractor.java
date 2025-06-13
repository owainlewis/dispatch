package com.dispatch.filters.ratelimit;

import com.dispatch.core.filter.FilterContext;
import com.dispatch.core.filter.HttpRequest;

@FunctionalInterface
public interface KeyExtractor {
    
    String extractKey(HttpRequest request, FilterContext context);
    
    static KeyExtractor byClientIp() {
        return (request, context) -> request.getClientIp();
    }
    
    static KeyExtractor byUserId() {
        return (request, context) -> {
            Object userId = context.getAttribute("user.id");
            return userId != null ? userId.toString() : request.getClientIp();
        };
    }
    
    static KeyExtractor byApiKey() {
        return (request, context) -> {
            String apiKey = request.getHeader("X-API-Key");
            if (apiKey == null) {
                apiKey = request.getQueryParam("api_key");
            }
            return apiKey != null ? apiKey : request.getClientIp();
        };
    }
    
    static KeyExtractor byHeader(String headerName) {
        return (request, context) -> {
            String value = request.getHeader(headerName);
            return value != null ? value : request.getClientIp();
        };
    }
    
    static KeyExtractor composite(KeyExtractor... extractors) {
        return (request, context) -> {
            StringBuilder key = new StringBuilder();
            for (KeyExtractor extractor : extractors) {
                if (key.length() > 0) {
                    key.append(":");
                }
                key.append(extractor.extractKey(request, context));
            }
            return key.toString();
        };
    }
}