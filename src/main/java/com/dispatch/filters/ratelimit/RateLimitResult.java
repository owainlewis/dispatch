package com.dispatch.filters.ratelimit;

public class RateLimitResult {
    private final boolean allowed;
    private final int remainingTokens;
    private final long resetTime;
    private final int limit;
    private final long retryAfterSeconds;
    
    public RateLimitResult(boolean allowed, int remainingTokens, long resetTime, int limit, long retryAfterSeconds) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
        this.resetTime = resetTime;
        this.limit = limit;
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public static RateLimitResult allowed(int remainingTokens, long resetTime, int limit) {
        return new RateLimitResult(true, remainingTokens, resetTime, limit, 0);
    }
    
    public static RateLimitResult denied(long resetTime, int limit, long retryAfterSeconds) {
        return new RateLimitResult(false, 0, resetTime, limit, retryAfterSeconds);
    }
    
    public boolean isAllowed() {
        return allowed;
    }
    
    public int getRemainingTokens() {
        return remainingTokens;
    }
    
    public long getResetTime() {
        return resetTime;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}