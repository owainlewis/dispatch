package com.dispatch.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FilterChain {
    private static final Logger logger = LoggerFactory.getLogger(FilterChain.class);
    
    private final List<GatewayFilter> filters;
    
    public FilterChain() {
        this.filters = new ArrayList<>();
    }
    
    public FilterChain(List<GatewayFilter> filters) {
        this.filters = new ArrayList<>(filters);
    }
    
    public void addFilter(GatewayFilter filter) {
        synchronized (filters) {
            filters.add(filter);
            logger.info("Added filter: {}", filter.getName());
        }
    }
    
    public void removeFilter(String filterName) {
        synchronized (filters) {
            filters.removeIf(filter -> filter.getName().equals(filterName));
            logger.info("Removed filter: {}", filterName);
        }
    }
    
    public List<GatewayFilter> getFilters() {
        synchronized (filters) {
            return new ArrayList<>(filters);
        }
    }
    
    public CompletableFuture<FilterResult> execute(HttpRequest request, FilterContext context) {
        List<GatewayFilter> applicableFilters = getApplicableFilters(request);
        
        if (applicableFilters.isEmpty()) {
            logger.warn("No applicable filters found for request: {} {}", request.method(), request.path());
            return CompletableFuture.completedFuture(
                FilterResult.error(404, "No applicable filters found")
            );
        }
        
        return executeFiltersSequentially(applicableFilters, request, context, 0);
    }
    
    private List<GatewayFilter> getApplicableFilters(HttpRequest request) {
        return filters.stream()
            .filter(filter -> {
                try {
                    return filter.shouldApply(request);
                } catch (Exception e) {
                    logger.error("Error checking if filter {} should apply", filter.getName(), e);
                    return false;
                }
            })
            .toList();
    }
    
    private CompletableFuture<FilterResult> executeFiltersSequentially(
            List<GatewayFilter> applicableFilters, 
            HttpRequest request, 
            FilterContext context, 
            int index) {
        
        if (index >= applicableFilters.size() || context.shouldTerminate()) {
            return CompletableFuture.completedFuture(FilterResult.proceed());
        }
        
        GatewayFilter filter = applicableFilters.get(index);
        
        logger.debug("Executing filter: {} for request: {} {}", 
            filter.getName(), request.method(), request.path());
        
        return filter.process(request, context)
            .thenCompose(result -> {
                if (result instanceof FilterResult.Respond) {
                    logger.debug("Filter {} returned response, stopping chain", filter.getName());
                    return CompletableFuture.completedFuture(result);
                } else if (result instanceof FilterResult.Proceed) {
                    return executeFiltersSequentially(applicableFilters, request, context, index + 1);
                } else {
                    logger.warn("Unknown filter result type from filter: {}", filter.getName());
                    return CompletableFuture.completedFuture(FilterResult.proceed());
                }
            })
            .exceptionally(throwable -> {
                logger.error("Filter {} threw exception", filter.getName(), throwable);
                return FilterResult.error(500, "Filter execution failed: " + filter.getName());
            });
    }
    
    public CompletableFuture<FilterResult> executeResponseFilters(
            HttpResponse response, 
            FilterContext context) {
        
        List<GatewayFilter> responseFilters = filters.stream()
            .filter(filter -> {
                try {
                    return filter.shouldApply(context.getOriginalRequest());
                } catch (Exception e) {
                    logger.error("Error checking if response filter {} should apply", filter.getName(), e);
                    return false;
                }
            })
            .toList();
        
        return executeResponseFiltersSequentially(responseFilters, response, context, 0);
    }
    
    private CompletableFuture<FilterResult> executeResponseFiltersSequentially(
            List<GatewayFilter> responseFilters,
            HttpResponse response,
            FilterContext context,
            int index) {
        
        if (index >= responseFilters.size()) {
            return CompletableFuture.completedFuture(FilterResult.respond(response));
        }
        
        GatewayFilter filter = responseFilters.get(index);
        
        logger.debug("Executing response filter: {}", filter.getName());
        
        return filter.processResponse(response, context)
            .thenCompose(result -> {
                if (result instanceof FilterResult.Respond respondResult) {
                    return executeResponseFiltersSequentially(
                        responseFilters, respondResult.response(), context, index + 1);
                } else {
                    return executeResponseFiltersSequentially(
                        responseFilters, response, context, index + 1);
                }
            })
            .exceptionally(throwable -> {
                logger.error("Response filter {} threw exception", filter.getName(), throwable);
                return FilterResult.respond(response);
            });
    }
    
    public int size() {
        synchronized (filters) {
            return filters.size();
        }
    }
    
    public boolean isEmpty() {
        synchronized (filters) {
            return filters.isEmpty();
        }
    }
}