package com.dispatch.filters.auth;

import java.util.List;
import java.util.Map;

public class UserInfo {
    private final String userId;
    private final String username;
    private final List<String> roles;
    private final Map<String, Object> attributes;
    
    public UserInfo(String userId, String username, List<String> roles, Map<String, Object> attributes) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.attributes = attributes;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public List<String> getRoles() {
        return roles;
    }
    
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    public Object getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }
}