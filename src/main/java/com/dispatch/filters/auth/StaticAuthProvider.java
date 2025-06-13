package com.dispatch.filters.auth;

import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.filter.HttpRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class StaticAuthProvider implements AuthProvider {
    
    private final Set<String> validTokens;
    private final Set<String> validApiKeys;
    
    @SuppressWarnings("unchecked")
    public StaticAuthProvider(DispatchConfig.FilterConfig config) {
        Map<String, Object> authConfig = config.getConfigMap("auth", Map.of());
        
        List<String> tokens = (List<String>) authConfig.getOrDefault("valid-tokens", List.of());
        List<String> apiKeys = (List<String>) authConfig.getOrDefault("valid-api-keys", List.of());
        
        this.validTokens = Set.copyOf(tokens);
        this.validApiKeys = Set.copyOf(apiKeys);
    }
    
    @Override
    public AuthResult authenticate(HttpRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return validateBearerToken(authHeader.substring(7));
        }
        
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null) {
            apiKey = request.getQueryParam("api_key");
        }
        
        if (apiKey != null) {
            return validateApiKey(apiKey);
        }
        
        return AuthResult.failure("No authentication credentials provided");
    }
    
    @Override
    public AuthResult validateBearerToken(String token) {
        if (validTokens.contains(token)) {
            UserInfo userInfo = new UserInfo(
                "static-user-" + token.hashCode(),
                "static-user",
                List.of("user"),
                Map.of("provider", "static", "token", token)
            );
            return AuthResult.success(userInfo);
        }
        
        return AuthResult.failure("Invalid bearer token");
    }
    
    @Override
    public AuthResult validateApiKey(String apiKey) {
        if (validApiKeys.contains(apiKey)) {
            UserInfo userInfo = new UserInfo(
                "static-api-" + apiKey.hashCode(),
                "static-api-user",
                List.of("api-user"),
                Map.of("provider", "static", "apiKey", apiKey)
            );
            return AuthResult.success(userInfo);
        }
        
        return AuthResult.failure("Invalid API key");
    }
}