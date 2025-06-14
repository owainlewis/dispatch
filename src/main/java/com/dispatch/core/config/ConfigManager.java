package com.dispatch.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigManager implements IConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    private final ObjectMapper yamlMapper;
    private DispatchConfig config;
    
    public ConfigManager() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    public DispatchConfig loadConfig(String configPath) throws IOException {
        logger.info("Loading configuration from: {}", configPath);
        
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            logger.warn("Configuration file not found: {}, loading default configuration", configPath);
            return loadDefaultConfig();
        }
        
        try {
            config = yamlMapper.readValue(configFile, DispatchConfig.class);
            validateConfig(config);
            logger.info("Configuration loaded successfully");
            return config;
        } catch (IOException e) {
            logger.error("Failed to load configuration from: {}", configPath, e);
            throw e;
        }
    }
    
    public DispatchConfig loadConfigFromClasspath(String resourcePath) throws IOException {
        logger.info("Loading configuration from classpath: {}", resourcePath);
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.warn("Configuration resource not found: {}, loading default configuration", resourcePath);
                return loadDefaultConfig();
            }
            
            config = yamlMapper.readValue(inputStream, DispatchConfig.class);
            validateConfig(config);
            logger.info("Configuration loaded successfully from classpath");
            return config;
        } catch (IOException e) {
            logger.error("Failed to load configuration from classpath: {}", resourcePath, e);
            throw e;
        }
    }
    
    public DispatchConfig loadDefaultConfig() {
        logger.info("Loading default configuration");
        config = createDefaultConfig();
        return config;
    }
    
    public void saveConfig(DispatchConfig config, String configPath) throws IOException {
        logger.info("Saving configuration to: {}", configPath);
        
        Path path = Path.of(configPath);
        Files.createDirectories(path.getParent());
        
        try {
            yamlMapper.writeValue(path.toFile(), config);
            this.config = config;
            logger.info("Configuration saved successfully");
        } catch (IOException e) {
            logger.error("Failed to save configuration to: {}", configPath, e);
            throw e;
        }
    }
    
    public DispatchConfig getConfig() {
        return config;
    }
    
    public void updateConfig(DispatchConfig newConfig) {
        validateConfig(newConfig);
        this.config = newConfig;
        logger.info("Configuration updated in memory");
    }
    
    private void validateConfig(DispatchConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        if (config.getServer() == null) {
            throw new IllegalArgumentException("Server configuration cannot be null");
        }
        
        int port = config.getServer().getPort();
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid port number: " + port + ". Must be between 1 and 65535");
        }
        
        if (config.getFilters() == null) {
            throw new IllegalArgumentException("Filters configuration cannot be null");
        }
        
        for (DispatchConfig.FilterConfig filterConfig : config.getFilters()) {
            if (filterConfig.getName() == null || filterConfig.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Filter name cannot be null or empty");
            }
        }
        
        logger.debug("Configuration validation passed");
    }
    
    private DispatchConfig createDefaultConfig() {
        DispatchConfig config = new DispatchConfig();
        
        DispatchConfig.ServerConfig serverConfig = new DispatchConfig.ServerConfig();
        serverConfig.setPort(8080);
        config.setServer(serverConfig);
        
        return config;
    }
    
    public String toYamlString() throws IOException {
        if (config == null) {
            throw new IllegalStateException("No configuration loaded");
        }
        return yamlMapper.writeValueAsString(config);
    }
    
    public String toYamlString(DispatchConfig config) throws IOException {
        return yamlMapper.writeValueAsString(config);
    }
    
    public DispatchConfig fromYamlString(String yaml) throws IOException {
        return yamlMapper.readValue(yaml, DispatchConfig.class);
    }
    
    public Map<String, Object> getFilterConfig(String filterName) {
        if (config == null || config.getFilters() == null) {
            return Map.of();
        }
        
        return config.getFilters().stream()
            .filter(f -> filterName.equals(f.getName()))
            .findFirst()
            .map(DispatchConfig.FilterConfig::getConfig)
            .orElse(Map.of());
    }
    
    public boolean isFilterEnabled(String filterName) {
        if (config == null || config.getFilters() == null) {
            return false;
        }
        
        return config.getFilters().stream()
            .filter(f -> filterName.equals(f.getName()))
            .findFirst()
            .map(DispatchConfig.FilterConfig::isEnabled)
            .orElse(false);
    }
}