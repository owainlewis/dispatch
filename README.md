# Dispatch API Gateway

A high-performance, modular API Gateway built on Netty and Java Virtual Threads. Dispatch provides a simple plugin architecture for extending functionality while maintaining exceptional performance and low latency.

## Features

- **High Performance**: Built on Netty with virtual thread integration, targeting 50,000+ requests/second
- **Plugin Architecture**: Simple filter interface with order-based execution
- **Built-in Filters**: Logging, Authentication, Rate Limiting, Proxy, Header Transformation, Health Checks
- **Configuration**: YAML-based configuration with runtime management
- **Monitoring**: Built-in health check endpoints and structured logging
- **Load Balancing**: Multiple load balancing strategies for backend services

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd dispatch

# Build the project
mvn clean package

# Run with default configuration
java -jar target/dispatch-gateway-1.0.0-SNAPSHOT.jar

# Run with custom configuration
java -jar target/dispatch-gateway-1.0.0-SNAPSHOT.jar config/my-config.yml
```

The gateway will start on port 8080 by default.

### Test the Gateway

```bash
# Health check
curl http://localhost:8080/health

# Test with authentication (using default test token)
curl -H "Authorization: Bearer test-token" http://localhost:8080/api/test
```

## Configuration

Dispatch uses YAML configuration files. Here's a minimal example:

```yaml
dispatch:
  server:
    port: 8080
    ssl:
      enabled: false
  
  filters:
    - name: logging
      enabled: true
      config:
        level: INFO
    
    - name: authentication
      enabled: true
      config:
        type: bearer-token
        skip-paths: ["/health", "/public/*"]
    
    - name: proxy
      enabled: true
      config:
        routes:
          routes:
            - path: "/api/users/*"
              backend: "http://user-service:8080"
```

### Server Configuration

```yaml
dispatch:
  server:
    port: 8080              # Server port
    ssl:
      enabled: true         # Enable SSL/TLS
      keystore: keystore.p12
      keystore-password: secret
```

### Filter Configuration

Filters are executed in the order they are defined in the configuration.

#### Logging Filter

```yaml
- name: logging
  enabled: true
  config:
    level: INFO             # DEBUG, INFO, WARN, ERROR
    include-body: false     # Log request/response bodies
    include-headers: false  # Log headers
```

#### Authentication Filter

```yaml
- name: authentication
  enabled: true
  config:
    type: bearer-token      # bearer-token, api-key, custom
    skip-paths:             # Paths to skip authentication
      - "/health"
      - "/public/*"
    provider: default       # default, jwt, static
```

##### JWT Authentication

```yaml
- name: authentication
  enabled: true
  config:
    type: bearer-token
    provider: jwt
    jwt:
      secret: "your-jwt-secret"
      issuer: "your-issuer"
```

##### Static Token Authentication

```yaml
- name: authentication
  enabled: true
  config:
    type: bearer-token
    provider: static
    auth:
      valid-tokens:
        - "token-1"
        - "token-2"
      valid-api-keys:
        - "api-key-1"
        - "api-key-2"
```

#### Rate Limiting Filter

```yaml
- name: rate-limiting
  enabled: true
  config:
    requests-per-minute: 1000   # Rate limit
    burst-capacity: 100         # Burst capacity
    key-type: client-ip         # client-ip, user-id, api-key, custom
    custom-header: X-Client-ID  # For custom key type
```

#### Header Transformer Filter

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
      transform:
        X-User-Agent: "lowercase"
    response:
      add:
        X-Powered-By: "Dispatch Gateway"
      remove:
        - "Server"
```

#### Proxy Filter

```yaml
- name: proxy
  enabled: true
  config:
    connect-timeout: 5          # Connection timeout (seconds)
    request-timeout: 30         # Request timeout (seconds)
    max-retries: 3             # Maximum retries
    load-balancer: round-robin  # round-robin, random
    routes:
      routes:
        - path: "/api/users/*"
          backend: "http://user-service:8080"
          enabled: true
        
        - path: "/api/orders/*"
          backend:                    # Multiple backends for load balancing
            - "http://order-service-1:8080"
            - "http://order-service-2:8080"
          strip-prefix: "/api"        # Remove prefix before forwarding
          add-prefix: "/v1"           # Add prefix before forwarding
          enabled: true
```

## Health Checks

Dispatch provides built-in health check endpoints:

- `GET /health` - Overall health status
- `GET /health/ready` - Readiness check
- `GET /health/live` - Liveness check

Example response:

```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z",
  "uptime": "PT2H30M",
  "checks": {
    "memory": {
      "status": "UP",
      "message": "Memory usage: 45.2% (471859200/1073741824 bytes)",
      "usedBytes": 471859200,
      "maxBytes": 1073741824,
      "usagePercent": 45.2
    },
    "system": {
      "status": "UP",
      "message": "System load: 1.25 (processors: 8)",
      "systemLoadAverage": 1.25,
      "availableProcessors": 8
    }
  }
}
```

## Custom Filters

You can create custom filters by implementing the `GatewayFilter` interface:

```java
@Component
public class CustomFilter implements GatewayFilter {
    
    @Override
    public String getName() {
        return "custom-filter";
    }
    
    @Override
    public boolean shouldApply(HttpRequest request) {
        return request.path().startsWith("/api");
    }
    
    @Override
    public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            // Your custom logic here
            
            // Store data in context for downstream filters
            context.setAttribute("custom.data", "value");
            
            // Continue to next filter
            return FilterResult.proceed();
            
            // Or return a response immediately
            // return FilterResult.respond(HttpResponse.ok("Custom response"));
            
            // Or return an error
            // return FilterResult.error(400, "Custom error");
        });
    }
    
    @Override
    public CompletableFuture<FilterResult> processResponse(HttpResponse response, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            // Modify response if needed
            response.setHeader("X-Custom-Filter", "processed");
            return FilterResult.proceed();
        });
    }
}
```

## Performance

Dispatch is designed for high performance:

- **Target**: 50,000+ requests/second
- **Latency**: Sub-millisecond P99 latency
- **Concurrency**: 10,000+ concurrent connections
- **Memory**: Efficient memory usage with Netty's pooled buffers

### Performance Tuning

1. **JVM Settings**:
```bash
java -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Xms2g -Xmx2g \
     --enable-preview \
     -jar dispatch-gateway.jar
```

2. **Virtual Threads**: Dispatch automatically uses virtual threads for filter processing

3. **Connection Pooling**: HTTP client uses connection pooling for backend services

## Monitoring and Observability

### Logging

Dispatch uses structured logging with configurable levels:

```
2024-01-15 10:30:15.123 [virtual-thread-1] INFO  LoggingFilter - REQUEST 192.168.1.100 GET /api/users 
2024-01-15 10:30:15.145 [virtual-thread-1] INFO  LoggingFilter - RESPONSE 192.168.1.100 GET /api/users 200 22ms
```

### Metrics

Request metrics are automatically tracked and exposed via health endpoints.

## Development

### Building

```bash
mvn clean compile
```

### Testing

```bash
mvn test
```

### Running in Development

```bash
mvn exec:java -Dexec.mainClass="com.dispatch.DispatchGateway" -Dexec.args="src/main/resources/application.yml"
```

## Docker

```dockerfile
FROM openjdk:21-jdk-slim

COPY target/dispatch-gateway-1.0.0-SNAPSHOT.jar app.jar
COPY src/main/resources/application.yml application.yml

EXPOSE 8080

CMD ["java", "--enable-preview", "-jar", "app.jar", "application.yml"]
```

```bash
docker build -t dispatch-gateway .
docker run -p 8080:8080 dispatch-gateway
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for your changes
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.