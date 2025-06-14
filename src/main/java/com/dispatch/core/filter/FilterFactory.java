package com.dispatch.core.filter;

import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.config.RouteConfig;
import com.dispatch.filters.LoggingFilter;
import com.dispatch.filters.auth.AuthenticationFilter;
import com.dispatch.filters.ratelimit.RateLimitingFilter;
import com.dispatch.filters.transform.HeaderTransformerFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating filter instances from configuration
 */
public class FilterFactory {
    private static final Logger logger = LoggerFactory.getLogger(FilterFactory.class);
    
    /**
     * Create a filter from route-specific configuration
     */
    public static GatewayFilter createFilter(RouteConfig.FilterConfig filterConfig) {
        if (!filterConfig.isEnabled()) {
            return null;
        }
        
        String filterName = filterConfig.getName();
        
        return switch (filterName) {
            case "logging" -> new LoggingFilter(convertToDispatchFilterConfig(filterConfig));
            case "authentication" -> new AuthenticationFilter(convertToDispatchFilterConfig(filterConfig));
            case "rate-limiting" -> new RateLimitingFilter(convertToDispatchFilterConfig(filterConfig));
            case "header-transformer" -> new HeaderTransformerFilter(convertToDispatchFilterConfig(filterConfig));
            default -> {
                logger.warn("Unknown filter type: {}", filterName);
                yield null;
            }
        };
    }
    
    /**
     * Create a filter from global configuration
     */
    public static GatewayFilter createFilter(DispatchConfig.FilterConfig filterConfig) {
        if (!filterConfig.isEnabled()) {
            return null;
        }
        
        String filterName = filterConfig.getName();
        
        return switch (filterName) {
            case "logging" -> new LoggingFilter(filterConfig);
            case "authentication" -> new AuthenticationFilter(filterConfig);
            case "rate-limiting" -> new RateLimitingFilter(filterConfig);
            case "header-transformer" -> new HeaderTransformerFilter(filterConfig);
            default -> {
                logger.warn("Unknown filter type: {}", filterName);
                yield null;
            }
        };
    }
    
    /**
     * Convert RouteConfig.FilterConfig to DispatchConfig.FilterConfig for compatibility
     */
    private static DispatchConfig.FilterConfig convertToDispatchFilterConfig(RouteConfig.FilterConfig routeFilterConfig) {
        DispatchConfig.FilterConfig dispatchFilterConfig = new DispatchConfig.FilterConfig();
        dispatchFilterConfig.setName(routeFilterConfig.getName());
        dispatchFilterConfig.setEnabled(routeFilterConfig.isEnabled());
        dispatchFilterConfig.setConfig(routeFilterConfig.getConfig());
        return dispatchFilterConfig;
    }
}