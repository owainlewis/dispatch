# Dispatch API Gateway

A high-performance, modular API Gateway built on Netty and Java Virtual Threads. Dispatch provides a route-centric architecture with both HTTP proxy and static response backends.

## Features

- **High Performance**: Built on Netty with virtual thread integration, targeting 50,000+ requests/second
- **Route-Centric Architecture**: Simple route configuration with per-route filter chains
- **Multiple Backend Types**: HTTP proxy and static response backends
- **Built-in Filters**: Logging, Authentication, Rate Limiting, Header Transformation
- **Configuration**: YAML-based configuration with clear, explicit structure
- **Monitoring**: Built-in health check endpoints and structured logging

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
java -jar target/dispatch-gateway-1.0.0-SNAPSHOT.jar config.yml
```

The gateway will start on port 8080 by default.

### Test the Gateway

```bash
# Test static response
curl http://localhost:8080/hello

# Test health endpoint
curl http://localhost:8080/status

# Test proxy route (with authentication)
curl -H "Authorization: Bearer test-token" http://localhost:8080/api/v1/posts/1
```

## Configuration

Dispatch uses a route-centric YAML configuration with explicit backend types:

```yaml
server:
  port: 8080

global_filters:
  - name: logging
    enabled: true
    config:
      level: INFO

routes:
  # HTTP Proxy Route
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

  # Static Response Route
  - type: "static"
    path: "/hello"
    response:
      status: 200
      body: "Hello, World!"
      content_type: "text/plain"
      headers:
        X-Custom-Header: "dispatch-gateway"

  # JSON Static Response
  - type: "static"
    path: "/status"
    response:
      status: 200
      body: '{"status": "healthy", "version": "1.0.0"}'
      content_type: "application/json"
```

## Route Types

### Proxy Routes

Proxy routes forward requests to HTTP backends:

```yaml
- type: "proxy"
  path: "/api/users/*"
  backend: "https://api.example.com"
  strip-prefix: "/api"        # Remove prefix before forwarding
  add-prefix: "/v1"           # Add prefix before forwarding
  filters:
    - name: authentication
      enabled: true
    - name: rate-limiting
      enabled: true
      config:
        requests-per-minute: 1000
```

### Static Response Routes

Static routes return predefined responses without proxying:

```yaml
- type: "static"
  path: "/health"
  response:
    status: 200
    body: "OK"
    content_type: "text/plain"
    headers:
      Cache-Control: "no-cache"
```

## Filter Configuration

### Global Filters

Global filters apply to all routes:

```yaml
global_filters:
  - name: logging
    enabled: true
    config:
      level: INFO
```

### Route-Specific Filters

Route-specific filters apply only to individual routes:

```yaml
routes:
  - type: "proxy"
    path: "/api/*"
    backend: "https://api.example.com"
    filters:
      - name: authentication
        enabled: true
        config:
          required: true
      - name: header-transformer
        enabled: true
        config:
          request:
            add:
              X-Request-ID: "auto-generated"
```

## Built-in Filters

### Logging Filter

```yaml
- name: logging
  enabled: true
  config:
    level: INFO             # DEBUG, INFO, WARN, ERROR
```

### Authentication Filter

```yaml
- name: authentication
  enabled: true
  config:
    required: true          # Require authentication
    skip-paths:             # Optional: paths to skip
      - "/health"
      - "/public/*"
```

### Rate Limiting Filter

```yaml
- name: rate-limiting
  enabled: true
  config:
    requests-per-minute: 1000   # Rate limit
    burst-capacity: 100         # Burst capacity
```

### Header Transformer Filter

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

## Path Matching

Routes support flexible path matching:

- **Exact match**: `/api/users` matches only `/api/users`
- **Wildcard suffix**: `/api/users/*` matches `/api/users/123`, `/api/users/123/posts`
- **Pattern matching**: `/api/*/posts` matches `/api/users/posts`, `/api/orders/posts`

## Performance

Dispatch is designed for high performance:

- **Target**: 50,000+ requests/second
- **Latency**: Sub-millisecond P99 latency
- **Concurrency**: 10,000+ concurrent connections
- **Memory**: Efficient memory usage with Netty's pooled buffers

### Performance Tuning

```bash
# Recommended JVM settings
java -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Xms2g -Xmx2g \
     --enable-preview \
     -jar dispatch-gateway.jar
```

## Custom Filters

Create custom filters by implementing the `GatewayFilter` interface:

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
            context.setAttribute("custom.data", "value");
            return FilterResult.proceed();
        });
    }
}
```

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
mvn exec:java -Dexec.mainClass="com.dispatch.DispatchGateway" -Dexec.args="config.yml"
```

## Docker

```dockerfile
FROM openjdk:21-jdk-slim

COPY target/dispatch-gateway-1.0.0-SNAPSHOT.jar app.jar
COPY config.yml config.yml

EXPOSE 8080

CMD ["java", "--enable-preview", "-jar", "app.jar", "config.yml"]
```

```bash
docker build -t dispatch-gateway .
docker run -p 8080:8080 dispatch-gateway
```

## Architecture

Dispatch uses a simple, route-centric architecture:

```
Incoming Request → Route Matching → Filter Chain → Backend Handler
                                      ↓
                              [Global Filters] + [Route Filters]
                                      ↓
                              [Proxy Handler] or [Static Handler]
```

### Key Components

- **RouteManager**: Matches incoming requests to configured routes
- **FilterChain**: Executes global and route-specific filters in order
- **ProxyHandler**: Forwards requests to HTTP backends
- **StaticHandler**: Returns predefined static responses

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for your changes
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.