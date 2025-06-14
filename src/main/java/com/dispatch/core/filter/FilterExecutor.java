package com.dispatch.core.filter;

import com.dispatch.core.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Shared filter execution logic to avoid duplication between FilterChain and RouteManager
 */
public class FilterExecutor {
    private static final Logger logger = LoggerFactory.getLogger(FilterExecutor.class);
    
    /**
     * Execute a list of filters sequentially
     * 
     * @param filters The filters to execute
     * @param request The HTTP request
     * @param context The filter context
     * @return A CompletableFuture containing the final filter result
     */
    public static CompletableFuture<FilterResult> executeFilters(
            List<GatewayFilter> filters,
            HttpRequest request,
            FilterContext context) {
        return executeFiltersSequentially(filters, request, context, 0);
    }
    
    private static CompletableFuture<FilterResult> executeFiltersSequentially(
            List<GatewayFilter> filters,
            HttpRequest request,
            FilterContext context,
            int index) {
        
        if (index >= filters.size() || context.shouldTerminate()) {
            return CompletableFuture.completedFuture(FilterResult.proceed());
        }
        
        GatewayFilter filter = filters.get(index);
        
        logger.debug("Executing filter: {} for request: {} {}", 
            filter.getName(), request.method(), request.path());
        
        return filter.process(request, context)
            .thenCompose(result -> {
                if (result instanceof FilterResult.Respond) {
                    logger.debug("Filter {} returned response, stopping chain", filter.getName());
                    return CompletableFuture.completedFuture(result);
                } else if (result instanceof FilterResult.Proceed) {
                    return executeFiltersSequentially(filters, request, context, index + 1);
                } else {
                    logger.warn("Unknown filter result type from filter: {}", filter.getName());
                    return CompletableFuture.completedFuture(FilterResult.proceed());
                }
            })
            .exceptionally(throwable -> {
                logger.error("Filter {} threw exception", filter.getName(), throwable);
                return FilterResult.error(500, 
                    Constants.ERROR_BACKEND_UNAVAILABLE + ": " + filter.getName());
            });
    }
}