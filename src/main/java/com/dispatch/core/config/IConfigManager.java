package com.dispatch.core.config;

/**
 * Interface for configuration management operations
 */
public interface IConfigManager {
    
    /**
     * Load configuration from a file path
     * 
     * @param configPath The path to the configuration file
     * @return The loaded DispatchConfig
     * @throws ConfigurationException if configuration loading fails
     */
    DispatchConfig loadConfig(String configPath) throws ConfigurationException;
    
    /**
     * Load the default configuration
     * 
     * @return The default DispatchConfig
     */
    DispatchConfig loadDefaultConfig();
    
    /**
     * Save configuration to a file path
     * 
     * @param config The configuration to save
     * @param configPath The path where to save the configuration
     * @throws ConfigurationException if configuration saving fails
     */
    void saveConfig(DispatchConfig config, String configPath) throws ConfigurationException;
    
    /**
     * Validate a configuration object
     * 
     * @param config The configuration to validate
     * @throws ConfigurationException if validation fails
     */
    void validateConfig(DispatchConfig config) throws ConfigurationException;
}