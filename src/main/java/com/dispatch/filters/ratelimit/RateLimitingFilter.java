package com.dispatch.filters.ratelimit;

import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RateLimitingFilter implements GatewayFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private final RateLimiter rateLimiter;
    private final KeyExtractor keyExtractor;
    
    public RateLimitingFilter() {
        this(60, 10, KeyExtractor.byClientIp());
    }
    
    public RateLimitingFilter(DispatchConfig.FilterConfig config) {
        int requestsPerMinute = config.getConfigInt("requests-per-minute", 60);
        int burstCapacity = config.getConfigInt("burst-capacity", 10);
        String keyType = config.getConfigString("key-type", "client-ip");
        
        this.rateLimiter = new TokenBucketRateLimiter(requestsPerMinute, burstCapacity);
        this.keyExtractor = createKeyExtractor(keyType, config);
    }
    
    public RateLimitingFilter(int requestsPerMinute, int burstCapacity, KeyExtractor keyExtractor) {
        this.rateLimiter = new TokenBucketRateLimiter(requestsPerMinute, burstCapacity);
        this.keyExtractor = keyExtractor;
    }
    
    @Override
    public String getName() {
        return "rate-limiting";
    }
    
    @Override
    public boolean shouldApply(HttpRequest request) {
        return true;
    }
    
    @Override
    public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String key = keyExtractor.extractKey(request, context);
                RateLimitResult result = rateLimiter.tryAcquire(key);
                
                if (result.isAllowed()) {
                    logger.debug("Rate limit check passed for key: {} (remaining: {})", 
                        key, result.getRemainingTokens());
                    
                    context.setAttribute("rate-limit.key", key);
                    context.setAttribute("rate-limit.remaining", result.getRemainingTokens());
                    context.setAttribute("rate-limit.reset-time", result.getResetTime());
                    
                    return FilterResult.proceed();
                } else {
                    logger.warn("Rate limit exceeded for key: {} (retry after: {}s)", 
                        key, result.getRetryAfterSeconds());
                    
                    HttpResponse response = HttpResponse.tooManyRequests("Rate limit exceeded");
                    response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
                    response.setHeader("X-RateLimit-Remaining", "0");
                    response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetTime()));
                    response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
                    
                    return FilterResult.respond(response);
                }
                
            } catch (Exception e) {
                logger.error("Rate limiting error for request: {} {}", 
                    request.method(), request.path(), e);
                return FilterResult.proceed(); 
            }
        });
    }
    
    @Override
    public CompletableFuture<FilterResult> processResponse(HttpResponse response, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            Object remaining = context.getAttribute("rate-limit.remaining");
            Object resetTime = context.getAttribute("rate-limit.reset-time");
            
            if (remaining != null) {
                response.setHeader("X-RateLimit-Remaining", remaining.toString());
            }
            if (resetTime != null) {
                response.setHeader("X-RateLimit-Reset", resetTime.toString());
            }
            
            return FilterResult.proceed();
        });
    }
    
    private KeyExtractor createKeyExtractor(String keyType, DispatchConfig.FilterConfig config) {
        return switch (keyType.toLowerCase()) {
            case "client-ip" -> KeyExtractor.byClientIp();
            case "user-id" -> KeyExtractor.byUserId();
            case "api-key" -> KeyExtractor.byApiKey();
            case "custom" -> {
                String customHeader = config.getConfigString("custom-header", "X-Client-ID");
                yield KeyExtractor.byHeader(customHeader);
            }
            default -> {
                logger.warn("Unknown key type: {}, using client-ip", keyType);
                yield KeyExtractor.byClientIp();
            }
        };
    }
}