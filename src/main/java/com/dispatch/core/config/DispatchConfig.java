package com.dispatch.core.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class DispatchConfig {
    
    @JsonProperty("server")
    private ServerConfig server = new ServerConfig();
    
    @JsonProperty("filters")
    private List<FilterConfig> filters = List.of();
    
    public ServerConfig getServer() {
        return server;
    }
    
    public void setServer(ServerConfig server) {
        this.server = server;
    }
    
    public List<FilterConfig> getFilters() {
        return filters;
    }
    
    public void setFilters(List<FilterConfig> filters) {
        this.filters = filters;
    }
    
    public static class ServerConfig {
        @JsonProperty("port")
        private int port = 8080;
        
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
            this.config = config;
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
        
        @SuppressWarnings("unchecked")
        public List<String> getConfigStringList(String key, List<String> defaultValue) {
            Object value = config.get(key);
            if (value instanceof List<?> list) {
                return (List<String>) list;
            }
            return defaultValue;
        }
        
        @SuppressWarnings("unchecked")
        public Map<String, Object> getConfigMap(String key, Map<String, Object> defaultValue) {
            Object value = config.get(key);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            return defaultValue;
        }
    }
}