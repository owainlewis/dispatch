package com.dispatch.client;

import com.dispatch.core.filter.HttpRequest;
import com.dispatch.core.filter.HttpResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for backend HTTP client operations
 */
public interface IBackendClient {
    
    /**
     * Proxy an HTTP request to a backend service
     * 
     * @param request The HTTP request to proxy
     * @param backendUrl The backend service URL
     * @return A CompletableFuture containing the HTTP response
     */
    CompletableFuture<HttpResponse> proxyRequest(HttpRequest request, String backendUrl);
    
    /**
     * Shutdown the backend client and clean up resources
     */
    void shutdown();
}