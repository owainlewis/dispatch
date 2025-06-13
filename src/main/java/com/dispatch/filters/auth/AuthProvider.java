package com.dispatch.filters.auth;

import com.dispatch.core.filter.HttpRequest;

public interface AuthProvider {
    
    AuthResult authenticate(HttpRequest request);
    
    AuthResult validateBearerToken(String token);
    
    AuthResult validateApiKey(String apiKey);
}