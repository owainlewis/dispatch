package com.dispatch;

import com.dispatch.core.config.ConfigManager;
import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.route.RouteManager;
import com.dispatch.core.server.NettyServer;
import com.dispatch.monitoring.HealthCheckFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class DispatchGateway {
    private static final Logger logger = LoggerFactory.getLogger(DispatchGateway.class);
    
    private final ConfigManager configManager;
    private NettyServer server;
    private RouteManager routeManager;
    private DispatchConfig config;
    
    public DispatchGateway() {
        this.configManager = new ConfigManager();
    }
    
    public CompletableFuture<Void> start(String configPath) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting Dispatch Gateway...");
                
                config = configManager.loadConfig(configPath);
                routeManager = new RouteManager(config);
                
                server = new NettyServer(
                    config.getServer().getPort(),
                    config.getServer().getSsl().isEnabled(),
                    routeManager
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
                routeManager = new RouteManager(config);
                
                server = new NettyServer(
                    config.getServer().getPort(),
                    config.getServer().getSsl().isEnabled(),
                    routeManager
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
            
            if (routeManager != null) {
                routeManager.shutdown();
            }
            
            logger.info("Dispatch Gateway shutdown complete");
        });
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
    
    public RouteManager getRouteManager() {
        return routeManager;
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