package com.dispatch.filters.proxy;

import java.util.List;

public interface LoadBalancer {
    
    String selectBackend(List<String> backends);
}