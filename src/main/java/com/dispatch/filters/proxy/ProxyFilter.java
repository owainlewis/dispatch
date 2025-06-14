package com.dispatch.filters.proxy;

import com.dispatch.client.BackendClient;
import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ProxyFilter implements GatewayFilter {
    private static final Logger logger = LoggerFactory.getLogger(ProxyFilter.class);
    
    private final List<RouteConfig> routes;
    private final BackendClient backendClient;
    private final LoadBalancer loadBalancer;
    
    public ProxyFilter() {
        this.routes = List.of();
        this.backendClient = new BackendClient();
        this.loadBalancer = new RoundRobinLoadBalancer();
    }
    
    @SuppressWarnings("unchecked")
    public ProxyFilter(DispatchConfig.FilterConfig config) {
        List<Map<String, Object>> routeConfigs = (List<Map<String, Object>>) 
            config.getConfig().getOrDefault("routes", List.of());
        
        this.routes = routeConfigs.stream()
            .map(RouteConfig::fromMap)
            .toList();
        
        logger.info("ProxyFilter initialized with {} routes", this.routes.size());
        for (RouteConfig route : this.routes) {
            logger.info("  Route: {} -> {}", route.getPathPattern(), route.getBackends());
        }
        
        Duration connectTimeout = Duration.ofSeconds(config.getConfigInt("connect-timeout", 5));
        Duration requestTimeout = Duration.ofSeconds(config.getConfigInt("request-timeout", 30));
        int maxRetries = config.getConfigInt("max-retries", 3);
        
        this.backendClient = new BackendClient(connectTimeout, requestTimeout, maxRetries);
        
        String lbStrategy = config.getConfigString("load-balancer", "round-robin");
        this.loadBalancer = createLoadBalancer(lbStrategy);
    }
    
    public ProxyFilter(List<RouteConfig> routes, BackendClient backendClient, LoadBalancer loadBalancer) {
        this.routes = routes;
        this.backendClient = backendClient;
        this.loadBalancer = loadBalancer;
    }
    
    @Override
    public String getName() {
        return "proxy";
    }
    
    @Override
    public boolean shouldApply(HttpRequest request) {
        return findMatchingRoute(request) != null;
    }
    
    @Override
    public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
        RouteConfig route = findMatchingRoute(request);
        
        if (route == null) {
            logger.warn("No matching route found for request: {} {}", request.method(), request.path());
            return CompletableFuture.completedFuture(
                FilterResult.error(404, "No matching route found")
            );
        }
        
        String backend = loadBalancer.selectBackend(route.getBackends());
        if (backend == null) {
            logger.error("No available backend for route: {}", route.getPathPattern());
            return CompletableFuture.completedFuture(
                FilterResult.error(503, "No available backend services")
            );
        }
        
        String targetPath = transformPath(request.path(), route);
        HttpRequest transformedRequest = transformRequest(request, targetPath);
        
        logger.debug("Proxying request {} {} to backend: {}", 
            request.method(), request.path(), backend);
        
        context.setAttribute("proxy.route", route.getPathPattern());
        context.setAttribute("proxy.backend", backend);
        context.setAttribute("proxy.original-path", request.path());
        context.setAttribute("proxy.target-path", targetPath);
        
        return backendClient.proxyRequest(transformedRequest, backend)
            .thenApply(response -> {
                logger.debug("Received response from backend: {} (status: {})", 
                    backend, response.statusCode());
                return FilterResult.respond(response);
            })
            .exceptionally(throwable -> {
                logger.error("Proxy request failed for {} {} to {}", 
                    request.method(), request.path(), backend, throwable);
                return FilterResult.error(502, "Backend service error");
            });
    }
    
    private RouteConfig findMatchingRoute(HttpRequest request) {
        String path = request.path();
        
        return routes.stream()
            .filter(route -> matchesPath(path, route.getPathPattern()))
            .findFirst()
            .orElse(null);
    }
    
    private boolean matchesPath(String path, String pattern) {
        if (pattern.equals(path)) {
            return true;
        }
        
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.startsWith(prefix);
        }
        
        if (pattern.contains("*")) {
            return path.matches(pattern.replace("*", ".*"));
        }
        
        return false;
    }
    
    private String transformPath(String originalPath, RouteConfig route) {
        String pattern = route.getPathPattern();
        String stripPrefix = route.getStripPrefix();
        String addPrefix = route.getAddPrefix();
        
        String transformedPath = originalPath;
        
        if (stripPrefix != null && !stripPrefix.isEmpty() && transformedPath.startsWith(stripPrefix)) {
            transformedPath = transformedPath.substring(stripPrefix.length());
            if (!transformedPath.startsWith("/")) {
                transformedPath = "/" + transformedPath;
            }
        }
        
        if (addPrefix != null && !addPrefix.isEmpty()) {
            if (!addPrefix.startsWith("/")) {
                addPrefix = "/" + addPrefix;
            }
            if (!addPrefix.endsWith("/") && !transformedPath.startsWith("/")) {
                addPrefix += "/";
            }
            transformedPath = addPrefix + transformedPath;
        }
        
        if (pattern.endsWith("/*")) {
            String routePrefix = pattern.substring(0, pattern.length() - 2);
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
    
    private HttpRequest transformRequest(HttpRequest originalRequest, String newPath) {
        String newUri = newPath;
        int queryIndex = originalRequest.uri().indexOf('?');
        if (queryIndex != -1) {
            newUri += originalRequest.uri().substring(queryIndex);
        }
        
        return new HttpRequest(
            originalRequest.method(),
            newUri,
            originalRequest.headers(),
            originalRequest.body(),
            originalRequest.remoteAddress()
        );
    }
    
    private LoadBalancer createLoadBalancer(String strategy) {
        return switch (strategy.toLowerCase()) {
            case "round-robin" -> new RoundRobinLoadBalancer();
            case "random" -> new RandomLoadBalancer();
            default -> {
                logger.warn("Unknown load balancer strategy: {}, using round-robin", strategy);
                yield new RoundRobinLoadBalancer();
            }
        };
    }
    
    public void shutdown() {
        if (backendClient != null) {
            backendClient.shutdown();
        }
    }
}