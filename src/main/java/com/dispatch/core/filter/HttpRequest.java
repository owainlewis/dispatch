package com.dispatch.core.filter;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class HttpRequest {
    private final HttpMethod method;
    private final String uri;
    private final String path;
    private final HttpHeaders headers;
    private final byte[] body;
    private final Map<String, List<String>> queryParams;
    private final InetSocketAddress remoteAddress;
    
    public HttpRequest(HttpMethod method, String uri, HttpHeaders headers, byte[] body, InetSocketAddress remoteAddress) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body != null ? body.clone() : new byte[0];
        this.remoteAddress = remoteAddress;
        
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        this.path = decoder.path();
        this.queryParams = decoder.parameters();
    }
    
    public HttpMethod method() {
        return method;
    }
    
    public String uri() {
        return uri;
    }
    
    public String path() {
        return path;
    }
    
    public HttpHeaders headers() {
        return headers;
    }
    
    public String getHeader(String name) {
        return headers.get(name);
    }
    
    public byte[] body() {
        return body.clone();
    }
    
    public String bodyAsString() {
        return new String(body);
    }
    
    public Map<String, List<String>> queryParams() {
        return queryParams;
    }
    
    public String getQueryParam(String name) {
        List<String> values = queryParams.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }
    
    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }
    
    public String getClientIp() {
        String forwarded = getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        
        String realIp = getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        
        return remoteAddress.getAddress().getHostAddress();
    }
}