package com.dispatch.core.filter;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FilterChainTest {
    
    private FilterChain filterChain;
    private HttpRequest testRequest;
    private FilterContext testContext;
    
    @BeforeEach
    void setUp() {
        filterChain = new FilterChain();
        testRequest = new HttpRequest(
            HttpMethod.GET,
            "/test",
            new DefaultHttpHeaders(),
            new byte[0],
            new InetSocketAddress("127.0.0.1", 8080)
        );
        testContext = new FilterContext(testRequest);
    }
    
    @Test
    void testEmptyFilterChain() {
        CompletableFuture<FilterResult> result = filterChain.execute(testRequest, testContext);
        
        assertNotNull(result);
        FilterResult filterResult = result.join();
        assertTrue(filterResult instanceof FilterResult.Respond);
        FilterResult.Respond respond = (FilterResult.Respond) filterResult;
        assertEquals(404, respond.response().statusCode());
    }
    
    @Test
    void testSingleFilterProceed() {
        TestFilter filter = new TestFilter("test1", true, FilterResult.proceed());
        filterChain.addFilter(filter);
        
        FilterResult result = filterChain.execute(testRequest, testContext).join();
        
        assertTrue(result instanceof FilterResult.Proceed);
        assertEquals(1, filter.getCallCount());
    }
    
    @Test
    void testSingleFilterRespond() {
        HttpResponse response = HttpResponse.ok("test response");
        TestFilter filter = new TestFilter("test1", true, FilterResult.respond(response));
        filterChain.addFilter(filter);
        
        FilterResult result = filterChain.execute(testRequest, testContext).join();
        
        assertTrue(result instanceof FilterResult.Respond);
        FilterResult.Respond respond = (FilterResult.Respond) result;
        assertEquals("test response", respond.response().bodyAsString());
        assertEquals(1, filter.getCallCount());
    }
    
    @Test
    void testMultipleFiltersAllProceed() {
        TestFilter filter1 = new TestFilter("test1", true, FilterResult.proceed());
        TestFilter filter2 = new TestFilter("test2", true, FilterResult.proceed());
        TestFilter filter3 = new TestFilter("test3", true, FilterResult.proceed());
        
        filterChain.addFilter(filter1);
        filterChain.addFilter(filter2);
        filterChain.addFilter(filter3);
        
        FilterResult result = filterChain.execute(testRequest, testContext).join();
        
        assertTrue(result instanceof FilterResult.Proceed);
        assertEquals(1, filter1.getCallCount());
        assertEquals(1, filter2.getCallCount());
        assertEquals(1, filter3.getCallCount());
    }
    
    @Test
    void testFilterChainStopsOnRespond() {
        TestFilter filter1 = new TestFilter("test1", true, FilterResult.proceed());
        TestFilter filter2 = new TestFilter("test2", true, FilterResult.respond(HttpResponse.ok("early response")));
        TestFilter filter3 = new TestFilter("test3", true, FilterResult.proceed());
        
        filterChain.addFilter(filter1);
        filterChain.addFilter(filter2);
        filterChain.addFilter(filter3);
        
        FilterResult result = filterChain.execute(testRequest, testContext).join();
        
        assertTrue(result instanceof FilterResult.Respond);
        FilterResult.Respond respond = (FilterResult.Respond) result;
        assertEquals("early response", respond.response().bodyAsString());
        
        assertEquals(1, filter1.getCallCount());
        assertEquals(1, filter2.getCallCount());
        assertEquals(0, filter3.getCallCount()); // Should not be called
    }
    
    @Test
    void testFilterShouldApply() {
        TestFilter filter1 = new TestFilter("test1", false, FilterResult.proceed()); // Should not apply
        TestFilter filter2 = new TestFilter("test2", true, FilterResult.respond(HttpResponse.ok("response")));
        
        filterChain.addFilter(filter1);
        filterChain.addFilter(filter2);
        
        FilterResult result = filterChain.execute(testRequest, testContext).join();
        
        assertTrue(result instanceof FilterResult.Respond);
        assertEquals(0, filter1.getCallCount()); // Should not be called
        assertEquals(1, filter2.getCallCount());
    }
    
    @Test
    void testFilterException() {
        TestFilter filter1 = new TestFilter("test1", true, FilterResult.proceed());
        TestFilter errorFilter = new TestFilter("error", true, null) {
            @Override
            public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
                throw new RuntimeException("Test exception");
            }
        };
        TestFilter filter3 = new TestFilter("test3", true, FilterResult.proceed());
        
        filterChain.addFilter(filter1);
        filterChain.addFilter(errorFilter);
        filterChain.addFilter(filter3);
        
        FilterResult result = filterChain.execute(testRequest, testContext).join();
        
        assertTrue(result instanceof FilterResult.Respond);
        FilterResult.Respond respond = (FilterResult.Respond) result;
        assertEquals(500, respond.response().statusCode());
        
        assertEquals(1, filter1.getCallCount());
        assertEquals(0, filter3.getCallCount()); // Should not be called due to exception
    }
    
    private static class TestFilter implements GatewayFilter {
        private final String name;
        private final boolean shouldApply;
        private final FilterResult result;
        private final AtomicInteger callCount = new AtomicInteger(0);
        
        public TestFilter(String name, boolean shouldApply, FilterResult result) {
            this.name = name;
            this.shouldApply = shouldApply;
            this.result = result;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public boolean shouldApply(HttpRequest request) {
            return shouldApply;
        }
        
        @Override
        public CompletableFuture<FilterResult> process(HttpRequest request, FilterContext context) {
            callCount.incrementAndGet();
            return CompletableFuture.completedFuture(result);
        }
        
        public int getCallCount() {
            return callCount.get();
        }
    }
}