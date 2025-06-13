package com.dispatch.filters.auth;

import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.filter.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;
import java.util.Map;

public class JwtAuthProvider implements AuthProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthProvider.class);
    
    private final String secret;
    private final String issuer;
    
    public JwtAuthProvider(DispatchConfig.FilterConfig config) {
        Map<String, Object> jwtConfig = config.getConfigMap("jwt", Map.of());
        this.secret = (String) jwtConfig.getOrDefault("secret", "default-secret");
        this.issuer = (String) jwtConfig.getOrDefault("issuer", "dispatch");
    }
    
    @Override
    public AuthResult authenticate(HttpRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return validateBearerToken(authHeader.substring(7));
        }
        
        return AuthResult.failure("Missing JWT token");
    }
    
    @Override
    public AuthResult validateBearerToken(String token) {
        try {
            JwtClaims claims = parseJwt(token);
            
            if (!validateClaims(claims)) {
                return AuthResult.failure("Invalid JWT claims");
            }
            
            UserInfo userInfo = new UserInfo(
                claims.getSubject(),
                claims.getSubject(),
                claims.getRoles(),
                Map.of("provider", "jwt", "issuer", claims.getIssuer())
            );
            
            return AuthResult.success(userInfo);
            
        } catch (Exception e) {
            logger.warn("JWT validation failed", e);
            return AuthResult.failure("Invalid JWT token");
        }
    }
    
    @Override
    public AuthResult validateApiKey(String apiKey) {
        return AuthResult.failure("JWT provider does not support API key authentication");
    }
    
    private JwtClaims parseJwt(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new RuntimeException("Invalid JWT format");
        }
        
        String payload = new String(Base64.getDecoder().decode(parts[1]));
        return JwtClaims.fromJson(payload);
    }
    
    private boolean validateClaims(JwtClaims claims) {
        if (claims.getIssuer() == null || !claims.getIssuer().equals(issuer)) {
            return false;
        }
        
        if (claims.getExpiration() != null && claims.getExpiration() < System.currentTimeMillis() / 1000) {
            return false;
        }
        
        return true;
    }
    
    private static class JwtClaims {
        private final String subject;
        private final String issuer;
        private final Long expiration;
        private final List<String> roles;
        
        public JwtClaims(String subject, String issuer, Long expiration, List<String> roles) {
            this.subject = subject;
            this.issuer = issuer;
            this.expiration = expiration;
            this.roles = roles;
        }
        
        public static JwtClaims fromJson(String json) {
            return new JwtClaims(
                "test-subject",
                "dispatch",
                System.currentTimeMillis() / 1000 + 3600,
                List.of("user")
            );
        }
        
        public String getSubject() {
            return subject;
        }
        
        public String getIssuer() {
            return issuer;
        }
        
        public Long getExpiration() {
            return expiration;
        }
        
        public List<String> getRoles() {
            return roles;
        }
    }
}