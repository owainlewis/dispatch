package com.dispatch.filters.proxy;

import java.util.List;
import java.util.Random;

public class RandomLoadBalancer implements LoadBalancer {
    
    private final Random random = new Random();
    
    @Override
    public String selectBackend(List<String> backends) {
        if (backends == null || backends.isEmpty()) {
            return null;
        }
        
        if (backends.size() == 1) {
            return backends.get(0);
        }
        
        int index = random.nextInt(backends.size());
        return backends.get(index);
    }
}