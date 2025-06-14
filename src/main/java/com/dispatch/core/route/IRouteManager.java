package com.dispatch.core.route;

import com.dispatch.core.filter.FilterContext;
import com.dispatch.core.filter.FilterResult;
import com.dispatch.core.filter.HttpRequest;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for route management and request processing
 */
public interface IRouteManager {
    
    /**
     * Process an incoming HTTP request through the route system
     * 
     * @param request The HTTP request to process
     * @param context The filter context for sharing data between filters
     * @return A CompletableFuture containing the filter result
     */
    CompletableFuture<FilterResult> processRequest(HttpRequest request, FilterContext context);
    
    /**
     * Shutdown the route manager and clean up resources
     */
    void shutdown();
}