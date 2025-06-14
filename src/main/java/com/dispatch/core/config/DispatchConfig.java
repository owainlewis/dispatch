package com.dispatch.core.config;

import com.dispatch.core.Constants;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class DispatchConfig {
    
    @JsonProperty("server")
    private ServerConfig server = new ServerConfig();
    
    @JsonProperty("routes")
    private List<RouteConfig> routes = List.of();
    
    @JsonProperty("global_filters")
    private List<FilterConfig> globalFilters = List.of();
    
    public ServerConfig getServer() {
        return server;
    }
    
    public void setServer(ServerConfig server) {
        this.server = server;
    }
    
    public List<RouteConfig> getRoutes() {
        return routes;
    }
    
    public void setRoutes(List<RouteConfig> routes) {
        this.routes = routes != null ? routes : List.of();
    }
    
    public List<FilterConfig> getGlobalFilters() {
        return globalFilters;
    }
    
    public void setGlobalFilters(List<FilterConfig> globalFilters) {
        this.globalFilters = globalFilters != null ? globalFilters : List.of();
    }
    
    
    public static class ServerConfig {
        @JsonProperty("port")
        private int port = Constants.DEFAULT_SERVER_PORT;
        
        @JsonProperty("ssl")
        private SslConfig ssl = new SslConfig();
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public SslConfig getSsl() {
            return ssl;
        }
        
        public void setSsl(SslConfig ssl) {
            this.ssl = ssl;
        }
    }
    
    public static class SslConfig {
        @JsonProperty("enabled")
        private boolean enabled = false;
        
        @JsonProperty("keystore")
        private String keystore;
        
        @JsonProperty("keystore-password")
        private String keystorePassword;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getKeystore() {
            return keystore;
        }
        
        public void setKeystore(String keystore) {
            this.keystore = keystore;
        }
        
        public String getKeystorePassword() {
            return keystorePassword;
        }
        
        public void setKeystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
        }
    }
    
    // Using FilterConfig from RouteConfig
    public static class FilterConfig extends RouteConfig.FilterConfig {
        // Additional methods for backward compatibility
        @SuppressWarnings("unchecked")
        public List<String> getConfigStringList(String key, List<String> defaultValue) {
            Object value = getConfig().get(key);
            if (value instanceof List<?> list) {
                return (List<String>) list;
            }
            return defaultValue;
        }
        
        @SuppressWarnings("unchecked")
        public Map<String, Object> getConfigMap(String key, Map<String, Object> defaultValue) {
            Object value = getConfig().get(key);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            return defaultValue;
        }
    }
}