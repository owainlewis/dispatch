package com.dispatch.filters.transform;

import com.dispatch.core.config.DispatchConfig;
import com.dispatch.core.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HeaderTransformerFilter implements GatewayFilter {
    private static final Logger logger = LoggerFactory.getLogger(HeaderTransformerFilter.class);
    
    private final List<HeaderTransformation> requestTransformations;
    private final List<HeaderTransformation> responseTransformations;
    
    public HeaderTransformerFilter() {
        this.requestTransformations = getDefaultRequestTransformations();
        this.responseTransformations = getDefaultResponseTransformations();
    }
    
    @SuppressWarnings("unchecked")
    public HeaderTransformerFilter(DispatchConfig.FilterConfig config) {
        Map<String, Object> requestConfig = config.getConfigMap("request", Map.of());
        Map<String, Object> responseConfig = config.getConfigMap("response", Map.of());
        
        this.requestTransformations = parseTransformations(requestConfig);
        this.responseTransformations = parseTransformations(responseConfig);
    }
    
    @Override
    public String getName() {
        return "header-transformer";
    }
    
    @Override
    public boolean shouldApply(HttpRequest request) {
        return true;
    }
    
    @Override
    public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (HeaderTransformation transformation : requestTransformations) {
                    transformation.apply(request, context);
                }
                
                logger.debug("Applied {} request header transformations", requestTransformations.size());
                return FilterResult.proceed();
                
            } catch (Exception e) {
                logger.error("Error applying request header transformations", e);
                return FilterResult.proceed(); 
            }
        });
    }
    
    @Override
    public CompletableFuture<FilterResult> processResponse(HttpResponse response, FilterContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (HeaderTransformation transformation : responseTransformations) {
                    transformation.applyToResponse(response, context);
                }
                
                logger.debug("Applied {} response header transformations", responseTransformations.size());
                return FilterResult.proceed();
                
            } catch (Exception e) {
                logger.error("Error applying response header transformations", e);
                return FilterResult.proceed();
            }
        });
    }
    
    private List<HeaderTransformation> getDefaultRequestTransformations() {
        return List.of(
            new AddHeaderTransformation("X-Request-ID", () -> UUID.randomUUID().toString()),
            new AddForwardedForTransformation(),
            new RemoveHeaderTransformation("X-Internal-Secret")
        );
    }
    
    private List<HeaderTransformation> getDefaultResponseTransformations() {
        return List.of(
            new AddHeaderTransformation("X-Powered-By", () -> "Dispatch Gateway"),
            new RemoveHeaderTransformation("Server")
        );
    }
    
    @SuppressWarnings("unchecked")
    private List<HeaderTransformation> parseTransformations(Map<String, Object> config) {
        List<HeaderTransformation> transformations = new java.util.ArrayList<>();
        
        // Add headers
        Map<String, String> addHeaders = (Map<String, String>) config.getOrDefault("add", Map.of());
        addHeaders.forEach((name, value) -> 
            transformations.add(new AddHeaderTransformation(name, () -> value)));
        
        // Remove headers
        List<String> removeHeaders = (List<String>) config.getOrDefault("remove", List.of());
        removeHeaders.forEach(name -> 
            transformations.add(new RemoveHeaderTransformation(name)));
        
        // Transform headers
        Map<String, String> transformHeaders = (Map<String, String>) config.getOrDefault("transform", Map.of());
        transformHeaders.forEach((name, operation) -> {
            if ("uppercase".equals(operation)) {
                transformations.add(new TransformHeaderTransformation(name, String::toUpperCase));
            } else if ("lowercase".equals(operation)) {
                transformations.add(new TransformHeaderTransformation(name, String::toLowerCase));
            }
        });
        
        return transformations;
    }
    
    private interface HeaderTransformation {
        void apply(HttpRequest request, FilterContext context);
        
        default void applyToResponse(HttpResponse response, FilterContext context) {
        }
    }
    
    private static class AddHeaderTransformation implements HeaderTransformation {
        private final String headerName;
        private final java.util.function.Supplier<String> valueSupplier;
        
        public AddHeaderTransformation(String headerName, java.util.function.Supplier<String> valueSupplier) {
            this.headerName = headerName;
            this.valueSupplier = valueSupplier;
        }
        
        @Override
        public void apply(HttpRequest request, FilterContext context) {
            String value = valueSupplier.get();
            request.headers().set(headerName, value);
            context.setAttribute("header." + headerName, value);
        }
        
        @Override
        public void applyToResponse(HttpResponse response, FilterContext context) {
            String value = valueSupplier.get();
            response.setHeader(headerName, value);
        }
    }
    
    private static class RemoveHeaderTransformation implements HeaderTransformation {
        private final String headerName;
        
        public RemoveHeaderTransformation(String headerName) {
            this.headerName = headerName;
        }
        
        @Override
        public void apply(HttpRequest request, FilterContext context) {
            request.headers().remove(headerName);
        }
        
        @Override
        public void applyToResponse(HttpResponse response, FilterContext context) {
            response.headers().remove(headerName);
        }
    }
    
    private static class TransformHeaderTransformation implements HeaderTransformation {
        private final String headerName;
        private final java.util.function.Function<String, String> transformer;
        
        public TransformHeaderTransformation(String headerName, java.util.function.Function<String, String> transformer) {
            this.headerName = headerName;
            this.transformer = transformer;
        }
        
        @Override
        public void apply(HttpRequest request, FilterContext context) {
            String value = request.getHeader(headerName);
            if (value != null) {
                request.headers().set(headerName, transformer.apply(value));
            }
        }
        
        @Override
        public void applyToResponse(HttpResponse response, FilterContext context) {
            String value = response.getHeader(headerName);
            if (value != null) {
                response.setHeader(headerName, transformer.apply(value));
            }
        }
    }
    
    private static class AddForwardedForTransformation implements HeaderTransformation {
        @Override
        public void apply(HttpRequest request, FilterContext context) {
            String clientIp = request.getClientIp();
            String existing = request.getHeader("X-Forwarded-For");
            
            String newValue = existing != null ? existing + ", " + clientIp : clientIp;
            request.headers().set("X-Forwarded-For", newValue);
        }
    }
}