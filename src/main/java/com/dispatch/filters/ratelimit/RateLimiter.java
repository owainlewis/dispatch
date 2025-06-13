package com.dispatch.filters.ratelimit;

public interface RateLimiter {
    
    RateLimitResult tryAcquire(String key);
    
    void reset(String key);
    
    void resetAll();
}