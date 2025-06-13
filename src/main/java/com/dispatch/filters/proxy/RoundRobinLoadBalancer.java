package com.dispatch.filters.proxy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {
    
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public String selectBackend(List<String> backends) {
        if (backends == null || backends.isEmpty()) {
            return null;
        }
        
        if (backends.size() == 1) {
            return backends.get(0);
        }
        
        int index = Math.abs(counter.getAndIncrement()) % backends.size();
        return backends.get(index);
    }
}