# Dispatch API Gateway - Product Requirements Document

## Overview

Dispatch is a high-performance, modular API Gateway built on Netty and Java Virtual Threads. It provides a route-centric architecture with support for HTTP proxy and static response backends, along with flexible per-route filter chains.

## Core Requirements

### 1. High-Performance Foundation
- **Server**: Netty-based HTTP server with virtual thread integration
- **Target Performance**: 50,000+ requests/second, sub-millisecond latency
- **Concurrency**: Support 10,000+ concurrent connections
- **Memory**: Efficient memory usage with Netty's pooled buffers

### 2. Route-Centric Architecture
- **Route Types**: Support for "proxy" and "static" route types
- **Backend Flexibility**: HTTP proxy to backend services or static response generation
- **Path Matching**: Exact, wildcard, and pattern-based path matching
- **Per-Route Configuration**: Independent filter chains and settings per route

### 3. Filter System
- **Global Filters**: Apply to all routes
- **Route-Specific Filters**: Apply only to individual routes
- **Order-Based Execution**: Filters execute in the order they are defined
- **Async Processing**: All filters support async/non-blocking operations

### 4. Built-in Capabilities
- **Logging Filter**: Request/response logging with configurable levels
- **Authentication Filter**: Bearer token and API key support
- **Rate Limiting Filter**: Token bucket algorithm with per-client limits
- **Header Transformer**: Add, remove, or modify headers
- **Proxy Handler**: Route requests to backend services
- **Static Handler**: Return predefined static responses

## Technical Architecture

### 1. Core Components

```
Dispatch
├── NettyServer (HTTP/1.1, HTTP/2 support)
├── RouteManager (Route matching and filter chain orchestration)
├── FilterChain (Plugin execution pipeline)
├── ConfigManager (YAML configuration management)
└── BackendClient (HTTP client for proxying)
```

### 2. Request Flow

```
Incoming Request → Route Matching → Filter Chain → Backend Handler
                     ↓               ↓               ↓
                RouteManager → [Global] + [Route] → [Proxy] or [Static]
                                Filters           Backend Handler
```

### 3. Configuration Structure

```yaml
server:
  port: 8080

global_filters:
  - name: logging
    enabled: true
    config:
      level: INFO

routes:
  - type: "proxy"                    # HTTP proxy route
    path: "/api/v1/*"
    backend: "https://api.example.com"
    filters:
      - name: authentication
        enabled: true

  - type: "static"                   # Static response route
    path: "/health"
    response:
      status: 200
      body: "OK"
      content_type: "text/plain"
```

## Detailed Requirements

### 1. Route Management

#### Route Types

**Proxy Routes**: Forward requests to HTTP backends
```yaml
- type: "proxy"
  path: "/api/users/*"
  backend: "https://user-service.example.com"
  strip-prefix: "/api"          # Optional: remove prefix
  add-prefix: "/v1"             # Optional: add prefix
  enabled: true                 # Optional: default true
  timeout:                      # Optional: route-specific timeouts
    connect: 5000
    request: 30000
    max-retries: 3
```

**Static Routes**: Return predefined responses
```yaml
- type: "static"
  path: "/status"
  response:
    status: 200
    body: '{"status": "healthy", "version": "1.0.0"}'
    content_type: "application/json"
    headers:                    # Optional: custom headers
      Cache-Control: "no-cache"
```

#### Path Matching
- **Exact match**: `/api/users` matches only `/api/users`
- **Wildcard suffix**: `/api/users/*` matches `/api/users/123`, `/api/users/123/posts`
- **Pattern matching**: `/api/*/posts` matches `/api/users/posts`, `/api/orders/posts`

### 2. Filter System

#### Filter Interface
```java
public interface GatewayFilter {
    /**
     * Unique filter name for identification
     */
    String getName();
    
    /**
     * Determine if filter should process this request
     */
    boolean shouldApply(HttpRequest request);
    
    /**
     * Process the request asynchronously
     */
    CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context);
    
    /**
     * Optional: Process response
     */
    default CompletableFuture<FilterResult> processResponse(HttpResponse response, FilterContext context) {
        return CompletableFuture.completedFuture(FilterResult.proceed());
    }
}
```

#### Filter Context
```java
public class FilterContext {
    private final Map<String, Object> attributes;
    private final HttpRequest originalRequest;
    private final Instant startTime;
    private volatile boolean shouldTerminate;
    
    // Methods for sharing data between filters
    public void setAttribute(String key, Object value);
    public <T> T getAttribute(String key, Class<T> type);
    public void terminate(); // Stop filter chain execution
}
```

#### Filter Result
```java
public sealed interface FilterResult {
    // Continue to next filter
    static FilterResult proceed() { return new Proceed(); }
    
    // Return response immediately (skip remaining filters)
    static FilterResult respond(HttpResponse response) { return new Respond(response); }
    
    // Return error response
    static FilterResult error(int statusCode, String message) { 
        return new Respond(new HttpResponse(statusCode, message)); 
    }
    
    record Proceed() implements FilterResult {}
    record Respond(HttpResponse response) implements FilterResult {}
}
```

### 3. Built-in Filters

#### Logging Filter
```java
@Component
public class LoggingFilter implements GatewayFilter {
    // Log format: [timestamp] method path status duration
    // Configurable log levels: DEBUG, INFO, WARN, ERROR
}
```

Configuration:
```yaml
- name: logging
  enabled: true
  config:
    level: INFO             # DEBUG, INFO, WARN, ERROR
```

#### Authentication Filter
```java
@Component  
public class AuthenticationFilter implements GatewayFilter {
    // Support Bearer Token validation
    // Configurable skip paths for public endpoints
    // Context attribute injection for downstream filters
}
```

Configuration:
```yaml
- name: authentication
  enabled: true
  config:
    required: true          # Require authentication
    skip-paths:             # Optional: paths to skip
      - "/health"
      - "/public/*"
```

#### Rate Limiting Filter
```java
@Component
public class RateLimitingFilter implements GatewayFilter {
    // Token bucket algorithm
    // Per-client IP rate limiting
    // Configurable rates and burst capacity
}
```

Configuration:
```yaml
- name: rate-limiting
  enabled: true
  config:
    requests-per-minute: 1000   # Rate limit
    burst-capacity: 100         # Burst capacity
```

#### Header Transformer Filter
```java
@Component
public class HeaderTransformerFilter implements GatewayFilter {
    // Add headers: X-Forwarded-For, X-Request-ID, etc.
    // Remove sensitive headers
    // Transform header values
    // Separate request and response processing
}
```

Configuration:
```yaml
- name: header-transformer
  enabled: true
  config:
    request:
      add:
        X-Request-ID: "auto-generated"
        X-Custom-Header: "value"
      remove:
        - "X-Internal-Secret"
    response:
      add:
        X-Powered-By: "Dispatch Gateway"
      remove:
        - "Server"
```

### 4. Backend Handlers

#### Proxy Handler
```java
public class ProxyFilter implements GatewayFilter {
    // Route requests to HTTP backends
    // Path transformation (strip/add prefix)
    // Timeout and retry configuration
    // Proper error handling for backend failures
}
```

Features:
- HTTP/1.1 and HTTP/2 support
- Connection pooling and keep-alive
- Configurable timeouts and retries
- Proper header passthrough
- Request/response streaming

#### Static Response Handler
```java
public class StaticResponseFilter implements GatewayFilter {
    // Return predefined static responses
    // Support for custom headers
    // Content-Type and Content-Length handling
    // JSON and text response support
}
```

Features:
- Configurable status codes
- Custom response headers
- Multiple content types (text, JSON, HTML, etc.)
- Efficient static response generation

### 5. Configuration System

#### Complete Configuration Example
```yaml
server:
  port: 8080
  ssl:
    enabled: false
    keystore: classpath:keystore.p12
    keystore-password: changeit

# Global filters apply to all routes
global_filters:
  - name: logging
    enabled: true
    config:
      level: INFO

# Routes are processed in order
routes:
  # Static health check
  - type: "static"
    path: "/health"
    response:
      status: 200
      body: "OK"
      content_type: "text/plain"

  # API routes with authentication
  - type: "proxy"
    path: "/api/v1/*"
    backend: "https://jsonplaceholder.typicode.com"
    filters:
      - name: authentication
        enabled: true
        config:
          required: true
      - name: rate-limiting
        enabled: true
        config:
          requests-per-minute: 100

  # Public API routes
  - type: "proxy"
    path: "/public/*"
    backend: "https://httpbin.org"
    filters:
      - name: rate-limiting
        enabled: true
        config:
          requests-per-minute: 1000

  # Static JSON responses
  - type: "static"
    path: "/version"
    response:
      status: 200
      body: '{"version": "1.0.0", "build": "12345"}'
      content_type: "application/json"
      headers:
        Cache-Control: "public, max-age=3600"
```

### 6. Backend Client

#### HTTP Client Requirements
- Use Java 21 HttpClient with virtual thread executor
- Connection pooling and keep-alive
- Configurable timeouts and retries
- Support for HTTP/1.1 and HTTP/2
- SSL/TLS support for HTTPS backends

#### Proxy Features
- Path-based routing to backend services
- Request/response header passthrough
- Path transformation (strip/add prefix)
- Proper error handling for backend failures

## Implementation Guidelines

### 1. Project Structure
```
src/main/java/com/dispatch/
├── core/
│   ├── server/           # Netty server implementation
│   ├── route/            # Route management and matching
│   ├── filter/           # Filter system
│   └── config/           # Configuration management
├── filters/              # Built-in filters
│   ├── auth/
│   ├── ratelimit/
│   ├── transform/
│   └── LoggingFilter.java
├── client/               # Backend HTTP client
└── monitoring/           # Health checks
```

### 2. Key Classes

#### RouteManager
```java
@Component
public class RouteManager {
    public CompletableFuture<FilterResult> processRequest(HttpRequest request, FilterContext context);
    private RouteConfig findMatchingRoute(String path);
    private List<GatewayFilter> createFilterChain(RouteConfig route);
}
```

#### RouteConfig
```java
public class RouteConfig {
    private String type;                    // "proxy" or "static"
    private String path;                    // Route path pattern
    private String backend;                 // For proxy routes
    private StaticResponseConfig response;  // For static routes
    private List<FilterConfig> filters;     // Route-specific filters
    
    public boolean isProxyRoute();
    public boolean isStaticRoute();
    public boolean matches(String requestPath);
    public String transformPath(String originalPath);
}
```

### 3. Plugin Development Example

#### Custom Authentication Filter
```java
@Component  
public class JwtAuthenticationFilter implements GatewayFilter {
    
    @Override
    public String getName() {
        return "jwt-auth";
    }
    
    @Override
    public boolean shouldApply(HttpRequest request) {
        return !request.path().startsWith("/public");
    }
    
    @Override
    public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return FilterResult.error(401, "Missing or invalid authorization header");
            }
            
            try {
                String token = authHeader.substring(7);
                JwtClaims claims = validateJwtToken(token);
                
                // Store user info in context for downstream filters
                context.setAttribute("user.id", claims.getUserId());
                context.setAttribute("user.roles", claims.getRoles());
                
                return FilterResult.proceed();
                
            } catch (JwtException e) {
                return FilterResult.error(401, "Invalid JWT token");
            }
        });
    }
}
```

## Acceptance Criteria

### Performance Requirements
- [ ] Handle 50,000+ requests/second on modern hardware
- [ ] Maintain sub-millisecond P99 latency for simple operations
- [ ] Support 10,000+ concurrent connections
- [ ] Memory usage remains stable under load

### Functionality Requirements
- [ ] Route-centric configuration with proxy and static backends
- [ ] Global and per-route filter chains
- [ ] Path matching with wildcards and patterns
- [ ] Built-in filters cover common gateway use cases
- [ ] Backend proxying with proper error handling
- [ ] Static response generation with custom headers

### Quality Requirements
- [ ] Comprehensive unit tests for core components
- [ ] Integration tests for route matching and filter execution
- [ ] Performance testing for target throughput
- [ ] Clear documentation with examples

### Usability Requirements
- [ ] Simple route and filter configuration
- [ ] Clear error messages and logging
- [ ] Configuration validation with helpful error messages
- [ ] Easy setup and getting started experience

## Success Metrics

### Technical Metrics
- **Throughput**: 50,000+ req/s
- **Latency**: P99 < 1ms for static responses, P99 < 5ms for proxy
- **Memory**: < 512MB for typical workload
- **Startup Time**: < 5 seconds

### Developer Experience
- **Route Configuration**: < 10 lines for basic proxy route
- **Filter Development**: < 50 lines for simple filter
- **Setup Time**: < 10 minutes from clone to running

## Future Considerations

### Phase 2 Features
- Multiple backends per proxy route with load balancing
- WebSocket proxying support
- HTTP/2 server push capabilities
- gRPC protocol support
- Circuit breaker pattern for backend health

### Performance Optimizations
- Zero-copy optimizations for large payloads
- Native image compilation with GraalVM
- Advanced connection pooling strategies
- Request/response streaming improvements

This PRD provides a comprehensive blueprint for Dispatch as a production-ready, high-performance API Gateway with a clean route-centric architecture supporting both proxy and static response backends.