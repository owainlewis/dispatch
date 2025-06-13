package com.dispatch.filters.ratelimit;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TokenBucketRateLimiter implements RateLimiter {
    
    private final int requestsPerMinute;
    private final int burstCapacity;
    private final ConcurrentMap<String, TokenBucket> buckets;
    
    public TokenBucketRateLimiter(int requestsPerMinute, int burstCapacity) {
        this.requestsPerMinute = requestsPerMinute;
        this.burstCapacity = burstCapacity;
        this.buckets = new ConcurrentHashMap<>();
    }
    
    @Override
    public RateLimitResult tryAcquire(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(requestsPerMinute, burstCapacity));
        return bucket.tryAcquire();
    }
    
    @Override
    public void reset(String key) {
        buckets.remove(key);
    }
    
    @Override
    public void resetAll() {
        buckets.clear();
    }
    
    private static class TokenBucket {
        private final int requestsPerMinute;
        private final int burstCapacity;
        private final double refillRate; // tokens per millisecond
        
        private volatile double tokens;
        private volatile long lastRefillTime;
        
        public TokenBucket(int requestsPerMinute, int burstCapacity) {
            this.requestsPerMinute = requestsPerMinute;
            this.burstCapacity = burstCapacity;
            this.refillRate = requestsPerMinute / 60000.0; // per millisecond
            this.tokens = burstCapacity;
            this.lastRefillTime = System.currentTimeMillis();
        }
        
        public synchronized RateLimitResult tryAcquire() {
            long now = System.currentTimeMillis();
            refill(now);
            
            long resetTime = calculateResetTime(now);
            
            if (tokens >= 1.0) {
                tokens -= 1.0;
                int remaining = (int) Math.floor(tokens);
                return RateLimitResult.allowed(remaining, resetTime, requestsPerMinute);
            } else {
                long retryAfterMs = (long) ((1.0 - tokens) / refillRate);
                long retryAfterSeconds = Math.max(1, retryAfterMs / 1000);
                return RateLimitResult.denied(resetTime, requestsPerMinute, retryAfterSeconds);
            }
        }
        
        private void refill(long now) {
            long timePassed = now - lastRefillTime;
            double tokensToAdd = timePassed * refillRate;
            
            tokens = Math.min(burstCapacity, tokens + tokensToAdd);
            lastRefillTime = now;
        }
        
        private long calculateResetTime(long now) {
            if (tokens >= burstCapacity) {
                return now / 1000 + 60; // Next minute
            }
            
            double tokensNeeded = burstCapacity - tokens;
            long timeToFill = (long) (tokensNeeded / refillRate);
            return (now + timeToFill) / 1000;
        }
    }
}