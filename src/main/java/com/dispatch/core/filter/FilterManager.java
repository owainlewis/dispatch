package com.dispatch.core.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FilterManager {
    private static final Logger logger = LoggerFactory.getLogger(FilterManager.class);
    
    private final List<GatewayFilter> filters;
    
    public FilterManager() {
        this.filters = new CopyOnWriteArrayList<>();
    }
    
    public void registerFilter(GatewayFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        
        if (filters.stream().anyMatch(f -> f.getName().equals(filter.getName()))) {
            throw new IllegalArgumentException("Filter with name '" + filter.getName() + "' already exists");
        }
        
        filters.add(filter);
        logger.info("Registered filter: {}", filter.getName());
    }
    
    public void unregisterFilter(String filterName) {
        if (filterName == null || filterName.trim().isEmpty()) {
            throw new IllegalArgumentException("Filter name cannot be null or empty");
        }
        
        boolean removed = filters.removeIf(filter -> filter.getName().equals(filterName));
        if (removed) {
            logger.info("Unregistered filter: {}", filterName);
        } else {
            logger.warn("Filter not found for removal: {}", filterName);
        }
    }
    
    public List<GatewayFilter> getActiveFilters() {
        return List.copyOf(filters);
    }
    
    public GatewayFilter getFilter(String filterName) {
        return filters.stream()
            .filter(filter -> filter.getName().equals(filterName))
            .findFirst()
            .orElse(null);
    }
    
    public boolean hasFilter(String filterName) {
        return filters.stream()
            .anyMatch(filter -> filter.getName().equals(filterName));
    }
    
    public int getFilterCount() {
        return filters.size();
    }
    
    public FilterChain createFilterChain() {
        return new FilterChain(getActiveFilters());
    }
    
    public void clear() {
        filters.clear();
        logger.info("Cleared all filters");
    }
}