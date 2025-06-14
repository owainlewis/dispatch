package com.dispatch.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class RouteConfig {
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("backend")
    private String backend;
    
    @JsonProperty("backends")
    private List<String> backends;
    
    @JsonProperty("filters")
    private List<FilterConfig> filters = List.of();
    
    @JsonProperty("strip-prefix")
    private String stripPrefix;
    
    @JsonProperty("add-prefix")
    private String addPrefix;
    
    @JsonProperty("enabled")
    private boolean enabled = true;
    
    @JsonProperty("load-balancer")
    private String loadBalancer = "round-robin";
    
    @JsonProperty("timeout")
    private TimeoutConfig timeout = new TimeoutConfig();
    
    // Getters and setters
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getBackend() {
        return backend;
    }
    
    public void setBackend(String backend) {
        this.backend = backend;
    }
    
    public List<String> getBackends() {
        // Return single backend as list if backends not specified
        if (backends == null || backends.isEmpty()) {
            return backend != null ? List.of(backend) : List.of();
        }
        return backends;
    }
    
    public void setBackends(List<String> backends) {
        this.backends = backends;
    }
    
    public List<FilterConfig> getFilters() {
        return filters;
    }
    
    public void setFilters(List<FilterConfig> filters) {
        this.filters = filters != null ? filters : List.of();
    }
    
    public String getStripPrefix() {
        return stripPrefix;
    }
    
    public void setStripPrefix(String stripPrefix) {
        this.stripPrefix = stripPrefix;
    }
    
    public String getAddPrefix() {
        return addPrefix;
    }
    
    public void setAddPrefix(String addPrefix) {
        this.addPrefix = addPrefix;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getLoadBalancer() {
        return loadBalancer;
    }
    
    public void setLoadBalancer(String loadBalancer) {
        this.loadBalancer = loadBalancer;
    }
    
    public TimeoutConfig getTimeout() {
        return timeout;
    }
    
    public void setTimeout(TimeoutConfig timeout) {
        this.timeout = timeout != null ? timeout : new TimeoutConfig();
    }
    
    /**
     * Check if this route matches the given path
     */
    public boolean matches(String requestPath) {
        if (path.equals(requestPath)) {
            return true;
        }
        
        if (path.endsWith("/*")) {
            String prefix = path.substring(0, path.length() - 2);
            return requestPath.startsWith(prefix);
        }
        
        if (path.contains("*")) {
            return requestPath.matches(path.replace("*", ".*"));
        }
        
        return false;
    }
    
    /**
     * Transform the request path according to route configuration
     */
    public String transformPath(String originalPath) {
        String transformedPath = originalPath;
        
        // Strip prefix if configured
        if (stripPrefix != null && !stripPrefix.isEmpty() && transformedPath.startsWith(stripPrefix)) {
            transformedPath = transformedPath.substring(stripPrefix.length());
            if (!transformedPath.startsWith("/")) {
                transformedPath = "/" + transformedPath;
            }
        }
        
        // Add prefix if configured
        if (addPrefix != null && !addPrefix.isEmpty()) {
            if (!addPrefix.startsWith("/")) {
                addPrefix = "/" + addPrefix;
            }
            if (!addPrefix.endsWith("/") && !transformedPath.startsWith("/")) {
                addPrefix += "/";
            }
            transformedPath = addPrefix + transformedPath;
        }
        
        // Handle wildcard paths
        if (path.endsWith("/*")) {
            String routePrefix = path.substring(0, path.length() - 2);
            if (originalPath.startsWith(routePrefix)) {
                String suffix = originalPath.substring(routePrefix.length());
                if (!suffix.startsWith("/")) {
                    suffix = "/" + suffix;
                }
                transformedPath = suffix;
            }
        }
        
        return transformedPath;
    }
    
    public static class FilterConfig {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("enabled")
        private boolean enabled = true;
        
        @JsonProperty("config")
        private Map<String, Object> config = Map.of();
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public Map<String, Object> getConfig() {
            return config;
        }
        
        public void setConfig(Map<String, Object> config) {
            this.config = config != null ? config : Map.of();
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getConfigValue(String key, Class<T> type, T defaultValue) {
            Object value = config.get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }
        
        public String getConfigString(String key, String defaultValue) {
            return getConfigValue(key, String.class, defaultValue);
        }
        
        public Integer getConfigInt(String key, Integer defaultValue) {
            Object value = config.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            return defaultValue;
        }
        
        public Boolean getConfigBoolean(String key, Boolean defaultValue) {
            return getConfigValue(key, Boolean.class, defaultValue);
        }
    }
    
    public static class TimeoutConfig {
        @JsonProperty("connect")
        private int connect = 5000; // 5 seconds
        
        @JsonProperty("request")
        private int request = 30000; // 30 seconds
        
        @JsonProperty("max-retries")
        private int maxRetries = 3;
        
        public int getConnect() {
            return connect;
        }
        
        public void setConnect(int connect) {
            this.connect = connect;
        }
        
        public int getRequest() {
            return request;
        }
        
        public void setRequest(int request) {
            this.request = request;
        }
        
        public int getMaxRetries() {
            return maxRetries;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }
}