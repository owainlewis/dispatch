package com.dispatch.core.filter;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.DefaultHttpHeaders;

public class HttpResponse {
    private final int statusCode;
    private final HttpHeaders headers;
    private final byte[] body;
    
    public HttpResponse(int statusCode, String body) {
        this(statusCode, new DefaultHttpHeaders(), body.getBytes());
    }
    
    public HttpResponse(int statusCode, byte[] body) {
        this(statusCode, new DefaultHttpHeaders(), body);
    }
    
    public HttpResponse(int statusCode, HttpHeaders headers, byte[] body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body != null ? body.clone() : new byte[0];
    }
    
    public int statusCode() {
        return statusCode;
    }
    
    public HttpHeaders headers() {
        return headers;
    }
    
    public byte[] body() {
        return body.clone();
    }
    
    public String bodyAsString() {
        return new String(body);
    }
    
    public void setHeader(String name, String value) {
        headers.set(name, value);
    }
    
    public void addHeader(String name, String value) {
        headers.add(name, value);
    }
    
    public String getHeader(String name) {
        return headers.get(name);
    }
    
    public static HttpResponse ok(String body) {
        return new HttpResponse(200, body);
    }
    
    public static HttpResponse badRequest(String message) {
        return new HttpResponse(400, message);
    }
    
    public static HttpResponse unauthorized(String message) {
        return new HttpResponse(401, message);
    }
    
    public static HttpResponse forbidden(String message) {
        return new HttpResponse(403, message);
    }
    
    public static HttpResponse notFound(String message) {
        return new HttpResponse(404, message);
    }
    
    public static HttpResponse tooManyRequests(String message) {
        return new HttpResponse(429, message);
    }
    
    public static HttpResponse internalServerError(String message) {
        return new HttpResponse(500, message);
    }
    
    public static HttpResponse badGateway(String message) {
        return new HttpResponse(502, message);
    }
    
    public static HttpResponse serviceUnavailable(String message) {
        return new HttpResponse(503, message);
    }
}