package com.dispatch.client;

import com.dispatch.core.filter.HttpRequest;
import com.dispatch.core.filter.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class BackendClient implements IBackendClient {
    private static final Logger logger = LoggerFactory.getLogger(BackendClient.class);
    
    private final HttpClient httpClient;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final int maxRetries;
    
    public BackendClient() {
        this(Duration.ofSeconds(5), Duration.ofSeconds(30), 3);
    }
    
    public BackendClient(Duration connectTimeout, Duration requestTimeout, int maxRetries) {
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.maxRetries = maxRetries;
        
        this.httpClient = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .connectTimeout(connectTimeout)
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }
    
    public CompletableFuture<HttpResponse> proxyRequest(HttpRequest request, String backendUrl) {
        return proxyRequest(request, backendUrl, 0);
    }
    
    private CompletableFuture<HttpResponse> proxyRequest(HttpRequest request, String backendUrl, int attempt) {
        try {
            URI targetUri = buildTargetUri(backendUrl, request.path(), request.uri());
            java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                .uri(targetUri)
                .timeout(requestTimeout);
            
            switch (request.method().name()) {
                case "GET" -> builder.GET();
                case "POST" -> builder.POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(request.body()));
                case "PUT" -> builder.PUT(java.net.http.HttpRequest.BodyPublishers.ofByteArray(request.body()));
                case "DELETE" -> builder.DELETE();
                case "HEAD" -> builder.method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody());
                case "OPTIONS" -> builder.method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody());
                case "PATCH" -> builder.method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofByteArray(request.body()));
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + request.method());
            }
            
            request.headers().forEach(entry -> {
                String name = entry.getKey();
                String value = entry.getValue();
                
                if (!isHopByHopHeader(name)) {
                    builder.header(name, value);
                }
            });
            
            java.net.http.HttpRequest httpRequest = builder.build();
            
            logger.debug("Proxying request {} {} to {}", 
                request.method(), request.path(), targetUri);
            
            return httpClient.sendAsync(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(this::convertResponse)
                .exceptionally(throwable -> handleError(throwable, request, backendUrl, attempt));
                
        } catch (Exception e) {
            logger.error("Error building proxy request for {}", backendUrl, e);
            return CompletableFuture.completedFuture(
                HttpResponse.badGateway("Failed to proxy request: " + e.getMessage())
            );
        }
    }
    
    private URI buildTargetUri(String backendUrl, String path, String originalUri) {
        try {
            URI baseUri = URI.create(backendUrl.endsWith("/") ? backendUrl.substring(0, backendUrl.length() - 1) : backendUrl);
            
            String query = null;
            int queryIndex = originalUri.indexOf('?');
            if (queryIndex != -1) {
                query = originalUri.substring(queryIndex + 1);
            }
            
            StringBuilder uriBuilder = new StringBuilder()
                .append(baseUri.getScheme()).append("://")
                .append(baseUri.getAuthority())
                .append(baseUri.getPath())
                .append(path);
            
            if (query != null) {
                uriBuilder.append("?").append(query);
            }
            
            return URI.create(uriBuilder.toString());
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid backend URL or path: " + backendUrl + path, e);
        }
    }
    
    
    private boolean isHopByHopHeader(String headerName) {
        return switch (headerName.toLowerCase()) {
            case "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
                 "te", "trailers", "transfer-encoding", "upgrade", "content-length", "host" -> true;
            default -> false;
        };
    }
    
    private boolean isHttp2PseudoHeader(String headerName) {
        return headerName.startsWith(":");
    }
    
    private HttpResponse convertResponse(java.net.http.HttpResponse<byte[]> httpResponse) {
        HttpResponse response = new HttpResponse(
            httpResponse.statusCode(),
            httpResponse.body()
        );
        
        httpResponse.headers().map().forEach((name, values) -> {
            if (!isHopByHopHeader(name) && !isHttp2PseudoHeader(name)) {
                values.forEach(value -> response.addHeader(name, value));
            }
        });
        
        return response;
    }
    
    private HttpResponse handleError(Throwable throwable, HttpRequest request, String backendUrl, int attempt) {
        logger.error("Proxy request failed (attempt {}/{}): {} {} to {}", 
            attempt + 1, maxRetries + 1, request.method(), request.path(), backendUrl, throwable);
        
        if (attempt < maxRetries && isRetryableError(throwable)) {
            logger.info("Retrying request {} {} to {} (attempt {}/{})", 
                request.method(), request.path(), backendUrl, attempt + 2, maxRetries + 1);
            
            try {
                Thread.sleep(Math.min(1000 * (1L << attempt), 5000)); // Exponential backoff, max 5s
                return proxyRequest(request, backendUrl, attempt + 1).join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return HttpResponse.serviceUnavailable("Request interrupted");
            }
        }
        
        if (throwable instanceof HttpTimeoutException) {
            return HttpResponse.serviceUnavailable("Backend service timeout");
        } else if (throwable instanceof java.net.ConnectException) {
            return HttpResponse.badGateway("Cannot connect to backend service");
        } else {
            return HttpResponse.badGateway("Backend service error: " + throwable.getMessage());
        }
    }
    
    private boolean isRetryableError(Throwable throwable) {
        return throwable instanceof HttpTimeoutException ||
               throwable instanceof java.net.ConnectException ||
               throwable instanceof java.io.IOException;
    }
    
    public void shutdown() {
        try {
            httpClient.close();
            logger.info("Backend client shutdown complete");
        } catch (Exception e) {
            logger.warn("Error during backend client shutdown", e);
        }
    }
}