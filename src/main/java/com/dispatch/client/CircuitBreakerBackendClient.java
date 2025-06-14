package com.dispatch.client;

import com.dispatch.core.circuit.CircuitBreaker;
import com.dispatch.core.error.StandardErrors;
import com.dispatch.core.filter.FilterResult;
import com.dispatch.core.filter.HttpRequest;
import com.dispatch.core.filter.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backend client with circuit breaker protection
 */
public class CircuitBreakerBackendClient implements IBackendClient {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerBackendClient.class);
    
    private final IBackendClient delegate;
    private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    public CircuitBreakerBackendClient(IBackendClient delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public CompletableFuture<HttpResponse> proxyRequest(HttpRequest request, String backendUrl) {
        CircuitBreaker circuitBreaker = getCircuitBreaker(backendUrl);
        
        if (!circuitBreaker.allowRequest()) {
            logger.warn("Circuit breaker is OPEN for backend: {}", backendUrl);
            return CompletableFuture.failedFuture(
                new RuntimeException("Circuit breaker is OPEN for backend: " + backendUrl));
        }
        
        return delegate.proxyRequest(request, backendUrl)
            .thenApply(response -> {
                // Consider 5xx responses as failures
                if (response.statusCode() >= 500) {
                    circuitBreaker.recordFailure();
                } else {
                    circuitBreaker.recordSuccess();
                }
                return response;
            })
            .exceptionally(throwable -> {
                circuitBreaker.recordFailure();
                throw new RuntimeException(throwable);
            });
    }
    
    @Override
    public void shutdown() {
        delegate.shutdown();
    }
    
    /**
     * Get or create a circuit breaker for the given backend URL
     */
    private CircuitBreaker getCircuitBreaker(String backendUrl) {
        return circuitBreakers.computeIfAbsent(backendUrl, k -> {
            logger.debug("Creating new circuit breaker for backend: {}", backendUrl);
            return new CircuitBreaker();
        });
    }
    
    /**
     * Get the circuit breaker state for a backend (for monitoring)
     */
    public CircuitBreaker.State getCircuitBreakerState(String backendUrl) {
        CircuitBreaker breaker = circuitBreakers.get(backendUrl);
        return breaker != null ? breaker.getState() : CircuitBreaker.State.CLOSED;
    }
    
    /**
     * Reset circuit breaker for a backend (for admin operations)
     */
    public void resetCircuitBreaker(String backendUrl) {
        CircuitBreaker breaker = circuitBreakers.get(backendUrl);
        if (breaker != null) {
            breaker.reset();
            logger.info("Reset circuit breaker for backend: {}", backendUrl);
        }
    }
}