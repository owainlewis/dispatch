package com.dispatch;

import com.dispatch.core.config.ConfigManager;
import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.filter.FilterManager;
import com.dispatch.core.filter.GatewayFilter;
import com.dispatch.core.server.NettyServer;
import com.dispatch.filters.LoggingFilter;
import com.dispatch.filters.auth.AuthenticationFilter;
import com.dispatch.filters.proxy.ProxyFilter;
import com.dispatch.filters.ratelimit.RateLimitingFilter;
import com.dispatch.filters.transform.HeaderTransformerFilter;
import com.dispatch.monitoring.HealthCheckFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class DispatchGateway {
    private static final Logger logger = LoggerFactory.getLogger(DispatchGateway.class);
    
    private final ConfigManager configManager;
    private final FilterManager filterManager;
    private NettyServer server;
    private DispatchConfig config;
    
    public DispatchGateway() {
        this.configManager = new ConfigManager();
        this.filterManager = new FilterManager();
    }
    
    public CompletableFuture<Void> start(String configPath) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting Dispatch Gateway...");
                
                config = configManager.loadConfig(configPath);
                registerFilters();
                
                server = new NettyServer(
                    config.getServer().getPort(),
                    config.getServer().getSsl().isEnabled(),
                    filterManager.createFilterChain()
                );
                
                server.start().join();
                
            } catch (Exception e) {
                logger.error("Failed to start Dispatch Gateway", e);
                throw new RuntimeException("Gateway startup failed", e);
            }
        });
    }
    
    public CompletableFuture<Void> startWithDefaultConfig() {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting Dispatch Gateway with default configuration...");
                
                config = configManager.loadDefaultConfig();
                registerDefaultFilters();
                
                server = new NettyServer(
                    config.getServer().getPort(),
                    config.getServer().getSsl().isEnabled(),
                    filterManager.createFilterChain()
                );
                
                server.start().join();
                
            } catch (Exception e) {
                logger.error("Failed to start Dispatch Gateway with default config", e);
                throw new RuntimeException("Gateway startup failed", e);
            }
        });
    }
    
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Shutting down Dispatch Gateway...");
            
            if (server != null) {
                server.shutdown().join();
            }
            
            filterManager.clear();
            
            logger.info("Dispatch Gateway shutdown complete");
        });
    }
    
    private void registerFilters() {
        filterManager.registerFilter(new HealthCheckFilter());
        
        for (DispatchConfig.FilterConfig filterConfig : config.getFilters()) {
            if (!filterConfig.isEnabled()) {
                logger.info("Skipping disabled filter: {}", filterConfig.getName());
                continue;
            }
            
            GatewayFilter filter = createFilter(filterConfig);
            if (filter != null) {
                filterManager.registerFilter(filter);
            }
        }
    }
    
    private void registerDefaultFilters() {
        filterManager.registerFilter(new HealthCheckFilter());
        filterManager.registerFilter(new LoggingFilter());
        filterManager.registerFilter(new HeaderTransformerFilter());
    }
    
    private GatewayFilter createFilter(DispatchConfig.FilterConfig filterConfig) {
        String filterName = filterConfig.getName();
        
        return switch (filterName) {
            case "logging" -> new LoggingFilter(filterConfig);
            case "authentication" -> new AuthenticationFilter(filterConfig);
            case "rate-limiting" -> new RateLimitingFilter(filterConfig);
            case "header-transformer" -> new HeaderTransformerFilter(filterConfig);
            case "proxy" -> new ProxyFilter(filterConfig);
            default -> {
                logger.warn("Unknown filter type: {}", filterName);
                yield null;
            }
        };
    }
    
    public boolean isRunning() {
        return server != null && server.isRunning();
    }
    
    public int getPort() {
        return server != null ? server.getPort() : -1;
    }
    
    public DispatchConfig getConfig() {
        return config;
    }
    
    public FilterManager getFilterManager() {
        return filterManager;
    }
    
    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "dispatch.yml";
        
        DispatchGateway gateway = new DispatchGateway();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
            gateway.shutdown().join();
        }));
        
        try {
            gateway.start(configPath).join();
        } catch (Exception e) {
            logger.error("Gateway failed to start", e);
            System.exit(1);
        }
    }
}