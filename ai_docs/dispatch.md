# Dispatch API Gateway - Product Requirements Document

## Overview

Dispatch is a high-performance, modular API Gateway built on Netty and Java Virtual Threads. It provides a simple plugin architecture for extending functionality while maintaining exceptional performance and low latency.

## Core Requirements

### 1. High-Performance Foundation
- **Server**: Netty-based HTTP server with virtual thread integration
- **Target Performance**: 50,000+ requests/second, sub-millisecond latency
- **Concurrency**: Support 10,000+ concurrent connections
- **Memory**: Efficient memory usage with Netty's pooled buffers

### 2. Plugin Architecture
- **Simple Plugin Interface**: Easy-to-implement filter/module system
- **Order-Based Execution**: Filters execute in the order they are registered/configured
- **Request/Response Processing**: Filters can modify both requests and responses
- **Async Processing**: All filters support async/non-blocking operations

### 3. Built-in Core Filters
- **Logging Filter**: Request/response logging with configurable levels
- **Authentication Filter**: Bearer token, API key, and custom auth support
- **Rate Limiting Filter**: Token bucket algorithm with per-client limits
- **Proxy Filter**: Route requests to backend services
- **Header Transformer**: Add, remove, or modify headers
- **Health Check Filter**: Built-in health endpoints

## Technical Architecture

### 1. Core Components

```
Dispatch
├── NettyServer (HTTP/1.1, HTTP/2 support)
├── FilterChain (Plugin execution pipeline)
├── FilterRegistry (Plugin management)
├── ConfigManager (Runtime configuration)
└── BackendClient (HTTP client for proxying)
```

### 2. Plugin Interface

```java
public interface GatewayFilter {
    String getName();
    boolean shouldApply(HttpRequest request);
    CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context);
    
    // Optional response processing
    default CompletableFuture<FilterResult> processResponse(HttpResponse response, FilterContext context) {
        return CompletableFuture.completedFuture(FilterResult.proceed());
    }
}
```

### 3. Request Flow

```
Incoming Request → Netty Handler → Filter Chain → Backend Proxy → Response
                                    ↓
                               [Filter 1] → [Filter 2] → [Filter N]
```

## Detailed Requirements

### 1. Server Implementation

#### Netty Server Setup
- Use Netty ServerBootstrap with NioEventLoopGroup
- HTTP/1.1 codec with aggregation for complete requests
- SSL/TLS support with configurable certificates
- Graceful shutdown handling

#### Virtual Thread Integration
- Process business logic in virtual threads
- Keep Netty event loop free for I/O operations
- Configurable thread pool for filter execution

#### Performance Optimizations
- Connection pooling for backend services
- HTTP keep-alive support
- Efficient request/response parsing
- Memory pool management

### 2. Filter System

#### Filter Interface Requirements
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

### 3. Built-in Filters

#### Logging Filter
```java
@Component
public class LoggingFilter implements GatewayFilter {
    // Log format: [timestamp] method path status duration
    // Configurable log levels: DEBUG, INFO, WARN, ERROR
    // Optional: Log request/response bodies
}
```

#### Authentication Filter
```java
@Component  
public class AuthenticationFilter implements GatewayFilter {
    // Support multiple auth types:
    // - Bearer Token validation
    // - API Key validation (header/query param)
    // - Custom authentication via pluggable AuthProvider
    // - Skip authentication for configured paths (e.g., /health, /public/*)
}
```

#### Rate Limiting Filter
```java
@Component
public class RateLimitingFilter implements GatewayFilter {
    // Token bucket algorithm
    // Configurable rates per client IP
    // Redis backend support for distributed rate limiting
    // Custom key extraction (IP, user ID, API key)
}
```

#### Header Transformer Filter
```java
@Component
public class HeaderTransformerFilter implements GatewayFilter {
    // Add headers: X-Forwarded-For, X-Request-ID, etc.
    // Remove sensitive headers from requests to backends
    // Transform header values (uppercase, lowercase, etc.)
    // Conditional header manipulation based on request properties
}
```

#### Proxy Filter
```java
@Component
public class ProxyFilter implements GatewayFilter {
    // Route requests to backend services based on path patterns
    // Support multiple load balancing strategies (round-robin, least-connections)
    // Circuit breaker pattern for backend health
    // Timeout and retry configuration
    // Request/response streaming for large payloads
}
```

### 4. Configuration System

#### Application Configuration
```yaml
dispatch:
  server:
    port: 8080
    ssl:
      enabled: false
      keystore: classpath:keystore.p12
      keystore-password: changeit
  
  # Filters execute in the order listed
  filters:
    - name: logging
      enabled: true
      config:
        level: INFO
        include-body: false
    
    - name: authentication
      enabled: true
      config:
        type: bearer-token
        skip-paths: ["/health", "/public/*"]
    
    - name: rate-limiting
      enabled: true
      config:
        requests-per-minute: 1000
        burst-capacity: 100
    
    - name: proxy
      enabled: true
      config:
        routes:
          - path: "/api/users/*"
            backend: "http://user-service:8080"
          - path: "/api/orders/*"
            backend: "http://order-service:8080"
```

#### Runtime Filter Registration
```java
// Simple filter management
@Service
public class FilterManager {
    public void registerFilter(GatewayFilter filter); // Adds to end of chain
    public void unregisterFilter(String filterName);
    public List<GatewayFilter> getActiveFilters(); // Returns in execution order
}
```

### 5. Backend Client

#### HTTP Client Requirements
- Use Java 21 HttpClient with virtual thread executor
- Connection pooling and keep-alive
- Configurable timeouts and retries
- Support for HTTP/1.1 and HTTP/2
- SSL/TLS support for HTTPS backends

#### Proxy Features
- Path-based routing to backend services
- Request/response header passthrough
- Proper error handling for backend failures
- Basic request/response streaming

### 6. Monitoring and Observability

#### Health Checks
- Built-in `/health` endpoint returning JSON status
- Simple UP/DOWN status based on server availability

#### Logging
- Structured logging with request method, path, status, duration
- Configurable log levels (DEBUG, INFO, WARN, ERROR)
- Optional request/response body logging for debugging

## Implementation Guidelines

### 1. Project Structure
```
src/main/java/com/dispatch/
├── core/
│   ├── server/           # Netty server implementation
│   ├── filter/           # Filter system
│   └── config/           # Configuration management
├── filters/              # Built-in filters
│   ├── auth/
│   ├── proxy/
│   ├── ratelimit/
│   └── transform/
├── client/               # Backend HTTP client
└── monitoring/           # Metrics and health checks
```

### 2. Dependencies
```xml
<dependencies>
    <!-- Core -->
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>4.1.100.Final</version>
    </dependency>
    
    <!-- Configuration -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.11</version>
    </dependency>
</dependencies>
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
        // Skip for public endpoints
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
    
    private JwtClaims validateJwtToken(String token) throws JwtException {
        // JWT validation logic
        return null;
    }
}

// Register filters in desired order
filterManager.registerFilter(new LoggingFilter());          // 1st
filterManager.registerFilter(new JwtAuthenticationFilter()); // 2nd  
filterManager.registerFilter(new RateLimitingFilter());     // 3rd
filterManager.registerFilter(new ProxyFilter());            // 4th (last)
```

## Acceptance Criteria

### Performance Requirements
- [ ] Handle 50,000+ requests/second on modern hardware
- [ ] Maintain sub-millisecond P99 latency for simple operations
- [ ] Support 10,000+ concurrent connections
- [ ] Memory usage remains stable under load

### Functionality Requirements
- [ ] Plugin system allows easy filter development
- [ ] Filters execute in registration order
- [ ] Configuration supports YAML setup
- [ ] Built-in filters cover common gateway use cases (auth, logging, proxy)
- [ ] Backend proxying with proper error handling works correctly
- [ ] Request and response processing through filter chain

### Quality Requirements
- [ ] Comprehensive unit tests for core components
- [ ] Integration tests for filter chain execution
- [ ] Performance testing for target throughput
- [ ] Clear documentation with filter development examples

### Usability Requirements
- [ ] Simple filter development with minimal boilerplate
- [ ] Clear error messages and logging
- [ ] Configuration validation with helpful error messages
- [ ] Easy setup and getting started experience

## Success Metrics

### Technical Metrics
- **Throughput**: 50,000+ req/s
- **Latency**: P99 < 1ms for auth + proxy
- **Memory**: < 512MB for typical workload
- **Startup Time**: < 5 seconds

### Developer Experience
- **Filter Development**: < 50 lines of code for simple filter
- **Setup Time**: < 10 minutes from clone to running
- **Documentation**: Complete API docs and tutorials

### Adoption Metrics
- GitHub stars and forks
- Maven Central download count
- Community contributions (filters, bug reports, PRs)

## Future Considerations

### Phase 2 Features
- WebSocket proxying support
- HTTP/2 server push capabilities
- gRPC protocol support
- Built-in service discovery
- Configuration API for runtime changes

### Performance Optimizations
- Zero-copy optimizations for large payloads
- Custom memory allocators
- Advanced connection pooling strategies
- Native image compilation with GraalVM

This PRD provides a comprehensive blueprint for building Dispatch as a production-ready, high-performance API Gateway with an intuitive plugin system.