package com.dispatch.filters.auth;

import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuthenticationFilter implements GatewayFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    
    private final AuthenticationType authType;
    private final List<String> skipPaths;
    private final AuthProvider authProvider;
    
    public AuthenticationFilter() {
        this(AuthenticationType.BEARER_TOKEN, List.of("/health", "/public/*"), new DefaultAuthProvider());
    }
    
    public AuthenticationFilter(DispatchConfig.FilterConfig config) {
        String typeStr = config.getConfigString("type", "bearer-token");
        this.authType = AuthenticationType.fromString(typeStr);
        this.skipPaths = config.getConfigStringList("skip-paths", List.of("/health", "/public/*"));
        this.authProvider = createAuthProvider(config);
    }
    
    public AuthenticationFilter(AuthenticationType authType, List<String> skipPaths, AuthProvider authProvider) {
        this.authType = authType;
        this.skipPaths = skipPaths;
        this.authProvider = authProvider;
    }
    
    @Override
    public String getName() {
        return "authentication";
    }
    
    @Override
    public boolean shouldApply(HttpRequest request) {
        String path = request.path();
        
        for (String skipPath : skipPaths) {
            if (pathMatches(path, skipPath)) {
                logger.debug("Skipping authentication for path: {}", path);
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AuthResult result = authenticate(request);
                
                if (result.isSuccess()) {
                    if (result.getUserInfo() != null) {
                        context.setAttribute("user.id", result.getUserInfo().getUserId());
                        context.setAttribute("user.roles", result.getUserInfo().getRoles());
                        context.setAttribute("user.info", result.getUserInfo());
                    }
                    
                    logger.debug("Authentication successful for request: {} {}", 
                        request.method(), request.path());
                    return FilterResult.proceed();
                } else {
                    logger.warn("Authentication failed for request: {} {} - {}", 
                        request.method(), request.path(), result.getErrorMessage());
                    return FilterResult.error(401, result.getErrorMessage());
                }
                
            } catch (Exception e) {
                logger.error("Authentication error for request: {} {}", 
                    request.method(), request.path(), e);
                return FilterResult.error(500, "Authentication service error");
            }
        });
    }
    
    private AuthResult authenticate(HttpRequest request) {
        return switch (authType) {
            case BEARER_TOKEN -> authenticateBearerToken(request);
            case API_KEY -> authenticateApiKey(request);
            case CUSTOM -> authProvider.authenticate(request);
        };
    }
    
    private AuthResult authenticateBearerToken(HttpRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return AuthResult.failure("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        return authProvider.validateBearerToken(token);
    }
    
    private AuthResult authenticateApiKey(HttpRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null) {
            apiKey = request.getQueryParam("api_key");
        }
        
        if (apiKey == null) {
            return AuthResult.failure("Missing API key");
        }
        
        return authProvider.validateApiKey(apiKey);
    }
    
    private boolean pathMatches(String path, String pattern) {
        if (pattern.equals(path)) {
            return true;
        }
        
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(prefix);
        }
        
        if (pattern.contains("*")) {
            return path.matches(pattern.replace("*", ".*"));
        }
        
        return false;
    }
    
    private AuthProvider createAuthProvider(DispatchConfig.FilterConfig config) {
        String providerType = config.getConfigString("provider", "default");
        
        return switch (providerType) {
            case "jwt" -> new JwtAuthProvider(config);
            case "static" -> new StaticAuthProvider(config);
            default -> new DefaultAuthProvider();
        };
    }
    
    public enum AuthenticationType {
        BEARER_TOKEN,
        API_KEY,
        CUSTOM;
        
        public static AuthenticationType fromString(String type) {
            return switch (type.toLowerCase()) {
                case "bearer-token", "bearer" -> BEARER_TOKEN;
                case "api-key", "apikey" -> API_KEY;
                case "custom" -> CUSTOM;
                default -> throw new IllegalArgumentException("Unknown authentication type: " + type);
            };
        }
    }
}