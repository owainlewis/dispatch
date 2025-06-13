package com.dispatch.core.filter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FilterContext {
    private final Map<String, Object> attributes;
    private final HttpRequest originalRequest;
    private final Instant startTime;
    private volatile boolean shouldTerminate;
    
    public FilterContext(HttpRequest originalRequest) {
        this.attributes = new ConcurrentHashMap<>();
        this.originalRequest = originalRequest;
        this.startTime = Instant.now();
        this.shouldTerminate = false;
    }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public void terminate() {
        this.shouldTerminate = true;
    }
    
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
    public HttpRequest getOriginalRequest() {
        return originalRequest;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public Map<String, Object> getAttributes() {
        return Map.copyOf(attributes);
    }
}