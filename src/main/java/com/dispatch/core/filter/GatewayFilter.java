package com.dispatch.core.filter;

import java.util.concurrent.CompletableFuture;

public interface GatewayFilter {
    
    String getName();
    
    boolean shouldApply(HttpRequest request);
    
    CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context);
    
    default CompletableFuture<FilterResult> processResponse(HttpResponse response, FilterContext context) {
        return CompletableFuture.completedFuture(FilterResult.proceed());
    }
}