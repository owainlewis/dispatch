package com.dispatch.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class RouteConfig {
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("backend")
    private String backend;
    
    @JsonProperty("response")
    private StaticResponseConfig response;
    
    @JsonProperty("filters")
    private List<FilterConfig> filters = List.of();
    
    @JsonProperty("strip-prefix")
    private String stripPrefix;
    
    @JsonProperty("add-prefix")
    private String addPrefix;
    
    @JsonProperty("enabled")
    private boolean enabled = true;
    
    
    // Getters and setters
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getBackend() {
        return backend;
    }
    
    public void setBackend(String backend) {
        this.backend = backend;
    }
    
    public StaticResponseConfig getResponse() {
        return response;
    }
    
    public void setResponse(StaticResponseConfig response) {
        this.response = response;
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
    
    
    /**
     * Check if this route is a proxy route
     */
    public boolean isProxyRoute() {
        return "proxy".equals(type);
    }
    
    /**
     * Check if this route is a static response route
     */
    public boolean isStaticRoute() {
        return "static".equals(type);
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
    
    public static class StaticResponseConfig {
        @JsonProperty("status")
        private int status = 200;
        
        @JsonProperty("body")
        private String body = "";
        
        @JsonProperty("headers")
        private Map<String, String> headers = Map.of();
        
        @JsonProperty("content_type")
        private String contentType = "text/plain";
        
        public int getStatus() {
            return status;
        }
        
        public void setStatus(int status) {
            this.status = status;
        }
        
        public String getBody() {
            return body;
        }
        
        public void setBody(String body) {
            this.body = body != null ? body : "";
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public void setHeaders(Map<String, String> headers) {
            this.headers = headers != null ? headers : Map.of();
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public void setContentType(String contentType) {
            this.contentType = contentType != null ? contentType : "text/plain";
        }
    }
}