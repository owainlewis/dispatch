package com.dispatch.core.circuit;

import com.dispatch.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker implementation to prevent cascade failures
 */
public class CircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
    
    private final int failureThreshold;
    private final long timeoutMs;
    private final long retryTimeoutMs;
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    
    public enum State {
        CLOSED,     // Normal operation
        OPEN,       // Circuit is open, requests fail fast
        HALF_OPEN   // Testing if service is back
    }
    
    public CircuitBreaker() {
        this(Constants.CIRCUIT_BREAKER_FAILURE_THRESHOLD, 
             Constants.CIRCUIT_BREAKER_TIMEOUT_MS, 
             Constants.CIRCUIT_BREAKER_RETRY_TIMEOUT_MS);
    }
    
    public CircuitBreaker(int failureThreshold, long timeoutMs, long retryTimeoutMs) {
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
        this.retryTimeoutMs = retryTimeoutMs;
    }
    
    /**
     * Check if the circuit breaker allows the request to proceed
     * 
     * @return true if request should proceed, false if it should fail fast
     */
    public boolean allowRequest() {
        State currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                if (shouldAttemptReset()) {
                    logger.info("Circuit breaker transitioning to HALF_OPEN");
                    state.set(State.HALF_OPEN);
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Record a successful request
     */
    public void recordSuccess() {
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            logger.info("Circuit breaker transitioning to CLOSED after successful request");
            state.set(State.CLOSED);
        }
        
        failureCount.set(0);
    }
    
    /**
     * Record a failed request
     */
    public void recordFailure() {
        lastFailureTime.set(Instant.now().toEpochMilli());
        int failures = failureCount.incrementAndGet();
        
        State currentState = state.get();
        
        if (currentState == State.HALF_OPEN) {
            logger.info("Circuit breaker transitioning to OPEN after failure in HALF_OPEN state");
            state.set(State.OPEN);
        } else if (currentState == State.CLOSED && failures >= failureThreshold) {
            logger.warn("Circuit breaker transitioning to OPEN after {} failures", failures);
            state.set(State.OPEN);
        }
    }
    
    /**
     * Get the current state of the circuit breaker
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * Get the current failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Check if we should attempt to reset the circuit breaker
     */
    private boolean shouldAttemptReset() {
        long currentTime = Instant.now().toEpochMilli();
        long timeSinceLastFailure = currentTime - lastFailureTime.get();
        return timeSinceLastFailure >= retryTimeoutMs;
    }
    
    /**
     * Manually reset the circuit breaker (for testing or admin purposes)
     */
    public void reset() {
        logger.info("Circuit breaker manually reset");
        state.set(State.CLOSED);
        failureCount.set(0);
        lastFailureTime.set(0);
    }
}