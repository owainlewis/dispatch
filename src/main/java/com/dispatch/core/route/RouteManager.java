package com.dispatch.core.route;

import com.dispatch.client.BackendClient;
import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.config.RouteConfig;
import com.dispatch.core.filter.*;
import com.dispatch.core.Constants;
import com.dispatch.core.error.StandardErrors;
import com.dispatch.core.filter.FilterExecutor;
import com.dispatch.core.filter.FilterFactory;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RouteManager implements IRouteManager {
    private static final Logger logger = LoggerFactory.getLogger(RouteManager.class);
    
    private final List<RouteConfig> routes;
    private final List<DispatchConfig.FilterConfig> globalFilters;
    private final BackendClient backendClient;
    
    public RouteManager(DispatchConfig config) {
        this.routes = new ArrayList<>(config.getRoutes());
        this.globalFilters = new ArrayList<>(config.getGlobalFilters());
        this.backendClient = new BackendClient();
    }
    
    public CompletableFuture<FilterResult> processRequest(HttpRequest request, FilterContext context) {
        // Find matching route
        RouteConfig matchingRoute = findMatchingRoute(request.path());
        
        if (matchingRoute == null) {
            logger.debug("No matching route found for path: {}", request.path());
            String requestId = context.getAttribute("requestId", String.class);
            return CompletableFuture.completedFuture(
                StandardErrors.noRouteFound(requestId)
            );
        }
        
        if (!matchingRoute.isEnabled()) {
            logger.debug("Route {} is disabled", matchingRoute.getPath());
            String requestId = context.getAttribute("requestId", String.class);
            return CompletableFuture.completedFuture(
                StandardErrors.backendUnavailable(requestId)
            );
        }
        
        logger.debug("Found matching route: {} for path: {}", matchingRoute.getPath(), request.path());
        
        // Store route info in context
        context.setAttribute("route.config", matchingRoute);
        context.setAttribute("route.path", matchingRoute.getPath());
        
        // Create combined filter chain (global + route-specific)
        List<GatewayFilter> filters = createFilterChain(matchingRoute);
        
        // Add backend handler at the end based on route type
        if (matchingRoute.isStaticRoute()) {
            filters.add(new StaticResponseFilter(matchingRoute));
        } else if (matchingRoute.isProxyRoute()) {
            filters.add(new ProxyFilter(matchingRoute));
        }
        
        // Execute filters sequentially using shared executor
        return FilterExecutor.executeFilters(filters, request, context);
    }
    
    private RouteConfig findMatchingRoute(String path) {
        return routes.stream()
            .filter(route -> route.matches(path))
            .findFirst()
            .orElse(null);
    }
    
    private List<GatewayFilter> createFilterChain(RouteConfig route) {
        List<GatewayFilter> filters = new ArrayList<>();
        
        // Add global filters first
        for (DispatchConfig.FilterConfig filterConfig : globalFilters) {
            GatewayFilter filter = FilterFactory.createFilter(filterConfig);
            if (filter != null) {
                filters.add(filter);
            }
        }
        
        // Add route-specific filters
        for (RouteConfig.FilterConfig filterConfig : route.getFilters()) {
            GatewayFilter filter = FilterFactory.createFilter(filterConfig);
            if (filter != null) {
                filters.add(filter);
            }
        }
        
        return filters;
    }
    
    
    
    public void shutdown() {
        if (backendClient != null) {
            backendClient.shutdown();
        }
    }
    
    /**
     * Internal static response filter implementation
     */
    private class StaticResponseFilter implements GatewayFilter {
        private final RouteConfig route;
        
        public StaticResponseFilter(RouteConfig route) {
            this.route = route;
        }
        
        @Override
        public String getName() {
            return "static-response-" + route.getPath();
        }
        
        @Override
        public boolean shouldApply(HttpRequest request) {
            return true; // Already matched by route
        }
        
        @Override
        public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
            RouteConfig.StaticResponseConfig staticConfig = route.getResponse();
            if (staticConfig == null) {
                String requestId = context.getAttribute("requestId", String.class);
                return CompletableFuture.completedFuture(
                    StandardErrors.internalServerError("Static response configuration missing", requestId)
                );
            }
            
            logger.debug("Returning static response for {} {} (status: {}, body: '{}')", 
                request.method(), request.path(), staticConfig.getStatus(), staticConfig.getBody());
            
            context.setAttribute("static.status", staticConfig.getStatus());
            context.setAttribute("static.body", staticConfig.getBody());
            context.setAttribute("static.content-type", staticConfig.getContentType());
            
            // Create response headers
            DefaultHttpHeaders responseHeaders = new DefaultHttpHeaders();
            staticConfig.getHeaders().forEach(responseHeaders::set);
            responseHeaders.set("Content-Type", staticConfig.getContentType());
            responseHeaders.set("Content-Length", String.valueOf(staticConfig.getBody().getBytes().length));
            
            HttpResponse response = new HttpResponse(
                staticConfig.getStatus(),
                responseHeaders,
                staticConfig.getBody().getBytes()
            );
            
            return CompletableFuture.completedFuture(FilterResult.respond(response));
        }
    }
    
    /**
     * Internal proxy filter implementation
     */
    private class ProxyFilter implements GatewayFilter {
        private final RouteConfig route;
        
        public ProxyFilter(RouteConfig route) {
            this.route = route;
        }
        
        @Override
        public String getName() {
            return "proxy-" + route.getPath();
        }
        
        @Override
        public boolean shouldApply(HttpRequest request) {
            return true; // Already matched by route
        }
        
        @Override
        public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
            String backend = route.getBackend();
            if (backend == null || backend.isEmpty()) {
                return CompletableFuture.completedFuture(
                    FilterResult.error(503, "No backend configured")
                );
            }
            
            String targetPath = route.transformPath(request.path());
            HttpRequest transformedRequest = transformRequest(request, targetPath);
            
            logger.debug("Proxying request {} {} to backend: {}", 
                request.method(), request.path(), backend);
            
            context.setAttribute("proxy.route", route.getPath());
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
                    String requestId = context.getAttribute("requestId", String.class);
                    return StandardErrors.backendUnavailable(requestId);
                });
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
    }
}