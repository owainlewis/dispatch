package com.dispatch.filters.auth;

import com.dispatch.core.filter.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class DefaultAuthProvider implements AuthProvider {
    private static final Logger logger = LoggerFactory.getLogger(DefaultAuthProvider.class);
    
    @Override
    public AuthResult authenticate(HttpRequest request) {
        logger.debug("Using default authentication - allowing all requests");
        return AuthResult.success();
    }
    
    @Override
    public AuthResult validateBearerToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return AuthResult.failure("Token cannot be null or empty");
        }
        
        if ("test-token".equals(token)) {
            UserInfo userInfo = new UserInfo(
                "test-user-id",
                "test-user",
                List.of("user"),
                Map.of("provider", "default")
            );
            return AuthResult.success(userInfo);
        }
        
        return AuthResult.failure("Invalid token");
    }
    
    @Override
    public AuthResult validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return AuthResult.failure("API key cannot be null or empty");
        }
        
        if ("test-api-key".equals(apiKey)) {
            UserInfo userInfo = new UserInfo(
                "api-user-id",
                "api-user",
                List.of("api-user"),
                Map.of("provider", "default", "apiKey", apiKey)
            );
            return AuthResult.success(userInfo);
        }
        
        return AuthResult.failure("Invalid API key");
    }
}