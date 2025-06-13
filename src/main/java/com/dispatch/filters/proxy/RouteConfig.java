package com.dispatch.filters.proxy;

import java.util.List;
import java.util.Map;

public class RouteConfig {
    private final String pathPattern;
    private final List<String> backends;
    private final String stripPrefix;
    private final String addPrefix;
    private final boolean enabled;
    
    public RouteConfig(String pathPattern, List<String> backends, String stripPrefix, String addPrefix, boolean enabled) {
        this.pathPattern = pathPattern;
        this.backends = backends;
        this.stripPrefix = stripPrefix;
        this.addPrefix = addPrefix;
        this.enabled = enabled;
    }
    
    @SuppressWarnings("unchecked")
    public static RouteConfig fromMap(Map<String, Object> config) {
        String pathPattern = (String) config.get("path");
        
        Object backendObj = config.get("backend");
        List<String> backends;
        if (backendObj instanceof String) {
            backends = List.of((String) backendObj);
        } else if (backendObj instanceof List) {
            backends = (List<String>) backendObj;
        } else {
            throw new IllegalArgumentException("Backend must be a string or list of strings");
        }
        
        String stripPrefix = (String) config.get("strip-prefix");
        String addPrefix = (String) config.get("add-prefix");
        boolean enabled = (Boolean) config.getOrDefault("enabled", true);
        
        return new RouteConfig(pathPattern, backends, stripPrefix, addPrefix, enabled);
    }
    
    public String getPathPattern() {
        return pathPattern;
    }
    
    public List<String> getBackends() {
        return backends;
    }
    
    public String getStripPrefix() {
        return stripPrefix;
    }
    
    public String getAddPrefix() {
        return addPrefix;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}