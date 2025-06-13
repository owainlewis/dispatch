package com.dispatch.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {
    
    private ConfigManager configManager;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        configManager = new ConfigManager();
    }
    
    @Test
    void testLoadDefaultConfig() {
        DispatchConfig config = configManager.loadDefaultConfig();
        
        assertNotNull(config);
        assertNotNull(config.getServer());
        assertEquals(8080, config.getServer().getPort());
        assertFalse(config.getServer().getSsl().isEnabled());
        assertNotNull(config.getFilters());
    }
    
    @Test
    void testLoadConfigFromFile() throws IOException {
        String yamlContent = """
            dispatch:
              server:
                port: 9090
                ssl:
                  enabled: true
                  keystore: test.p12
                  keystore-password: secret
              filters:
                - name: logging
                  enabled: true
                  config:
                    level: DEBUG
                - name: proxy
                  enabled: true
                  config:
                    routes:
                      - path: "/api/*"
                        backend: "http://backend:8080"
            """;
        
        Path configFile = tempDir.resolve("test-config.yml");
        Files.write(configFile, yamlContent.getBytes());
        
        DispatchConfig config = configManager.loadConfig(configFile.toString());
        
        assertNotNull(config);
        assertEquals(9090, config.getServer().getPort());
        assertTrue(config.getServer().getSsl().isEnabled());
        assertEquals("test.p12", config.getServer().getSsl().getKeystore());
        assertEquals("secret", config.getServer().getSsl().getKeystorePassword());
        
        assertEquals(2, config.getFilters().size());
        
        DispatchConfig.FilterConfig loggingFilter = config.getFilters().get(0);
        assertEquals("logging", loggingFilter.getName());
        assertTrue(loggingFilter.isEnabled());
        assertEquals("DEBUG", loggingFilter.getConfigString("level", "INFO"));
        
        DispatchConfig.FilterConfig proxyFilter = config.getFilters().get(1);
        assertEquals("proxy", proxyFilter.getName());
        assertTrue(proxyFilter.isEnabled());
    }
    
    @Test
    void testLoadNonExistentConfig() throws IOException {
        DispatchConfig config = configManager.loadConfig("non-existent-file.yml");
        
        // Should load default config when file doesn't exist
        assertNotNull(config);
        assertEquals(8080, config.getServer().getPort());
    }
    
    @Test
    void testSaveConfig() throws IOException {
        DispatchConfig config = new DispatchConfig();
        config.getServer().setPort(9090);
        
        DispatchConfig.FilterConfig filterConfig = new DispatchConfig.FilterConfig();
        filterConfig.setName("test-filter");
        filterConfig.setEnabled(true);
        filterConfig.setConfig(Map.of("key", "value"));
        
        config.setFilters(List.of(filterConfig));
        
        Path configFile = tempDir.resolve("saved-config.yml");
        configManager.saveConfig(config, configFile.toString());
        
        assertTrue(Files.exists(configFile));
        
        // Load it back and verify
        DispatchConfig loadedConfig = configManager.loadConfig(configFile.toString());
        assertEquals(9090, loadedConfig.getServer().getPort());
        assertEquals(1, loadedConfig.getFilters().size());
        assertEquals("test-filter", loadedConfig.getFilters().get(0).getName());
    }
    
    @Test
    void testConfigValidation() {
        // Test null config
        assertThrows(IllegalArgumentException.class, () -> {
            configManager.updateConfig(null);
        });
        
        // Test invalid port
        DispatchConfig invalidConfig = new DispatchConfig();
        invalidConfig.getServer().setPort(70000); // Invalid port
        
        assertThrows(IllegalArgumentException.class, () -> {
            configManager.updateConfig(invalidConfig);
        });
        
        // Test filter with null name
        DispatchConfig configWithInvalidFilter = new DispatchConfig();
        DispatchConfig.FilterConfig invalidFilter = new DispatchConfig.FilterConfig();
        invalidFilter.setName(null);
        configWithInvalidFilter.setFilters(List.of(invalidFilter));
        
        assertThrows(IllegalArgumentException.class, () -> {
            configManager.updateConfig(configWithInvalidFilter);
        });
    }
    
    @Test
    void testFilterConfigHelperMethods() {
        DispatchConfig.FilterConfig filterConfig = new DispatchConfig.FilterConfig();
        filterConfig.setConfig(Map.of(
            "stringValue", "test",
            "intValue", 42,
            "boolValue", true,
            "listValue", List.of("a", "b", "c"),
            "mapValue", Map.of("nested", "value")
        ));
        
        assertEquals("test", filterConfig.getConfigString("stringValue", "default"));
        assertEquals("default", filterConfig.getConfigString("missing", "default"));
        
        assertEquals(42, filterConfig.getConfigInt("intValue", 0));
        assertEquals(0, filterConfig.getConfigInt("missing", 0));
        
        assertTrue(filterConfig.getConfigBoolean("boolValue", false));
        assertFalse(filterConfig.getConfigBoolean("missing", false));
        
        List<String> list = filterConfig.getConfigStringList("listValue", List.of());
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        
        Map<String, Object> map = filterConfig.getConfigMap("mapValue", Map.of());
        assertEquals("value", map.get("nested"));
    }
    
    @Test
    void testYamlStringConversion() throws IOException {
        DispatchConfig config = new DispatchConfig();
        config.getServer().setPort(8081);
        
        String yamlString = configManager.toYamlString(config);
        assertNotNull(yamlString);
        assertTrue(yamlString.contains("port: 8081"));
        
        DispatchConfig parsedConfig = configManager.fromYamlString(yamlString);
        assertEquals(8081, parsedConfig.getServer().getPort());
    }
    
    @Test
    void testFilterConfigQueries() {
        DispatchConfig config = new DispatchConfig();
        
        DispatchConfig.FilterConfig filter1 = new DispatchConfig.FilterConfig();
        filter1.setName("filter1");
        filter1.setEnabled(true);
        filter1.setConfig(Map.of("key1", "value1"));
        
        DispatchConfig.FilterConfig filter2 = new DispatchConfig.FilterConfig();
        filter2.setName("filter2");
        filter2.setEnabled(false);
        filter2.setConfig(Map.of("key2", "value2"));
        
        config.setFilters(List.of(filter1, filter2));
        configManager.updateConfig(config);
        
        // Test getFilterConfig
        Map<String, Object> filter1Config = configManager.getFilterConfig("filter1");
        assertEquals("value1", filter1Config.get("key1"));
        
        Map<String, Object> missingConfig = configManager.getFilterConfig("missing");
        assertTrue(missingConfig.isEmpty());
        
        // Test isFilterEnabled
        assertTrue(configManager.isFilterEnabled("filter1"));
        assertFalse(configManager.isFilterEnabled("filter2"));
        assertFalse(configManager.isFilterEnabled("missing"));
    }
}