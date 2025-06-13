package com.dispatch.filters.auth;

import com.dispatch.core.filter.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationFilterTest {
    
    private AuthenticationFilter authFilter;
    private FilterContext context;
    
    @BeforeEach
    void setUp() {
        authFilter = new AuthenticationFilter(
            AuthenticationFilter.AuthenticationType.BEARER_TOKEN,
            List.of("/health", "/public/*"),
            new DefaultAuthProvider()
        );
    }
    
    @Test
    void testShouldApplySkipsHealthEndpoint() {
        HttpRequest healthRequest = createRequest(HttpMethod.GET, "/health");
        assertFalse(authFilter.shouldApply(healthRequest));
    }
    
    @Test
    void testShouldApplySkipsPublicPaths() {
        HttpRequest publicRequest = createRequest(HttpMethod.GET, "/public/api");
        assertFalse(authFilter.shouldApply(publicRequest));
    }
    
    @Test
    void testShouldApplyForProtectedPaths() {
        HttpRequest protectedRequest = createRequest(HttpMethod.GET, "/api/users");
        assertTrue(authFilter.shouldApply(protectedRequest));
    }
    
    @Test
    void testValidBearerToken() {
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.set("Authorization", "Bearer test-token");
        
        HttpRequest request = createRequest(HttpMethod.GET, "/api/users", headers);
        context = new FilterContext(request);
        
        FilterResult result = authFilter.process(request, context).join();
        
        assertTrue(result instanceof FilterResult.Proceed);
        assertEquals("test-user-id", context.getAttribute("user.id"));
        assertNotNull(context.getAttribute("user.roles"));
    }
    
    @Test
    void testInvalidBearerToken() {
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.set("Authorization", "Bearer invalid-token");
        
        HttpRequest request = createRequest(HttpMethod.GET, "/api/users", headers);
        context = new FilterContext(request);
        
        FilterResult result = authFilter.process(request, context).join();
        
        assertTrue(result instanceof FilterResult.Respond);
        FilterResult.Respond respond = (FilterResult.Respond) result;
        assertEquals(401, respond.response().statusCode());
    }
    
    @Test
    void testMissingAuthorizationHeader() {
        HttpRequest request = createRequest(HttpMethod.GET, "/api/users");
        context = new FilterContext(request);
        
        FilterResult result = authFilter.process(request, context).join();
        
        assertTrue(result instanceof FilterResult.Respond);
        FilterResult.Respond respond = (FilterResult.Respond) result;
        assertEquals(401, respond.response().statusCode());
        assertTrue(respond.response().bodyAsString().contains("Missing or invalid"));
    }
    
    @Test
    void testInvalidAuthorizationHeaderFormat() {
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.set("Authorization", "Basic dGVzdDp0ZXN0"); // Base64 encoded test:test
        
        HttpRequest request = createRequest(HttpMethod.GET, "/api/users", headers);
        context = new FilterContext(request);
        
        FilterResult result = authFilter.process(request, context).join();
        
        assertTrue(result instanceof FilterResult.Respond);
        FilterResult.Respond respond = (FilterResult.Respond) result;
        assertEquals(401, respond.response().statusCode());
    }
    
    @Test
    void testApiKeyAuthentication() {
        AuthenticationFilter apiKeyFilter = new AuthenticationFilter(
            AuthenticationFilter.AuthenticationType.API_KEY,
            List.of("/health"),
            new DefaultAuthProvider()
        );
        
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.set("X-API-Key", "test-api-key");
        
        HttpRequest request = createRequest(HttpMethod.GET, "/api/users", headers);
        context = new FilterContext(request);
        
        FilterResult result = apiKeyFilter.process(request, context).join();
        
        assertTrue(result instanceof FilterResult.Proceed);
        assertEquals("api-user-id", context.getAttribute("user.id"));
    }
    
    @Test
    void testApiKeyInQueryParameter() {
        AuthenticationFilter apiKeyFilter = new AuthenticationFilter(
            AuthenticationFilter.AuthenticationType.API_KEY,
            List.of("/health"),
            new DefaultAuthProvider()
        );
        
        HttpRequest request = createRequest(HttpMethod.GET, "/api/users?api_key=test-api-key");
        context = new FilterContext(request);
        
        FilterResult result = apiKeyFilter.process(request, context).join();
        
        assertTrue(result instanceof FilterResult.Proceed);
        assertEquals("api-user-id", context.getAttribute("user.id"));
    }
    
    private HttpRequest createRequest(HttpMethod method, String uri) {
        return createRequest(method, uri, new DefaultHttpHeaders());
    }
    
    private HttpRequest createRequest(HttpMethod method, String uri, DefaultHttpHeaders headers) {
        return new HttpRequest(
            method,
            uri,
            headers,
            new byte[0],
            new InetSocketAddress("127.0.0.1", 8080)
        );
    }
}