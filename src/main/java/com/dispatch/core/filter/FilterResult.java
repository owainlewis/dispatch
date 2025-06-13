package com.dispatch.core.filter;

public sealed interface FilterResult {
    
    static FilterResult proceed() { 
        return new Proceed(); 
    }
    
    static FilterResult respond(HttpResponse response) { 
        return new Respond(response); 
    }
    
    static FilterResult error(int statusCode, String message) { 
        return new Respond(new HttpResponse(statusCode, message)); 
    }
    
    record Proceed() implements FilterResult {}
    record Respond(HttpResponse response) implements FilterResult {}
}