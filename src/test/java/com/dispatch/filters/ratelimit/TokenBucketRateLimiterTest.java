package com.dispatch.filters.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {
    
    private TokenBucketRateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new TokenBucketRateLimiter(60, 10); // 60 requests per minute, 10 burst
    }
    
    @Test
    void testInitialBurstCapacity() {
        String key = "test-key";
        
        // Should allow up to burst capacity immediately
        for (int i = 0; i < 10; i++) {
            RateLimitResult result = rateLimiter.tryAcquire(key);
            assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
            assertEquals(9 - i, result.getRemainingTokens());
        }
        
        // Next request should be denied
        RateLimitResult result = rateLimiter.tryAcquire(key);
        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemainingTokens());
    }
    
    @Test
    void testTokenRefill() throws InterruptedException {
        String key = "test-key";
        
        // Exhaust initial tokens
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire(key).isAllowed());
        }
        
        // Should be denied immediately
        assertFalse(rateLimiter.tryAcquire(key).isAllowed());
        
        // Wait for some tokens to refill (1 second = 1 token at 60/minute rate)
        Thread.sleep(1100); // Slightly more than 1 second
        
        // Should now allow one request
        assertTrue(rateLimiter.tryAcquire(key).isAllowed());
        
        // But not a second one immediately
        assertFalse(rateLimiter.tryAcquire(key).isAllowed());
    }
    
    @Test
    void testDifferentKeys() {
        String key1 = "key1";
        String key2 = "key2";
        
        // Exhaust tokens for key1
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire(key1).isAllowed());
        }
        assertFalse(rateLimiter.tryAcquire(key1).isAllowed());
        
        // key2 should still have full capacity
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire(key2).isAllowed());
        }
        assertFalse(rateLimiter.tryAcquire(key2).isAllowed());
    }
    
    @Test
    void testReset() {
        String key = "test-key";
        
        // Exhaust tokens
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire(key).isAllowed());
        }
        assertFalse(rateLimiter.tryAcquire(key).isAllowed());
        
        // Reset the key
        rateLimiter.reset(key);
        
        // Should have full capacity again
        assertTrue(rateLimiter.tryAcquire(key).isAllowed());
    }
    
    @Test
    void testResetAll() {
        String key1 = "key1";
        String key2 = "key2";
        
        // Use some tokens
        assertTrue(rateLimiter.tryAcquire(key1).isAllowed());
        assertTrue(rateLimiter.tryAcquire(key2).isAllowed());
        
        // Reset all
        rateLimiter.resetAll();
        
        // Both keys should have full capacity
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire(key1).isAllowed());
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire(key2).isAllowed());
        }
    }
    
    @Test
    void testRetryAfterCalculation() {
        String key = "test-key";
        
        // Exhaust tokens
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.tryAcquire(key).isAllowed());
        }
        
        RateLimitResult result = rateLimiter.tryAcquire(key);
        assertFalse(result.isAllowed());
        assertTrue(result.getRetryAfterSeconds() > 0);
        assertTrue(result.getRetryAfterSeconds() <= 60); // Should be reasonable
    }
    
    @Test
    void testRateLimitResultFields() {
        String key = "test-key";
        
        RateLimitResult result = rateLimiter.tryAcquire(key);
        
        assertTrue(result.isAllowed());
        assertEquals(9, result.getRemainingTokens());
        assertEquals(60, result.getLimit());
        assertTrue(result.getResetTime() > 0);
        assertEquals(0, result.getRetryAfterSeconds());
    }
}