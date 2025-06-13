package com.dispatch.filters.auth;

public class AuthResult {
    private final boolean success;
    private final String errorMessage;
    private final UserInfo userInfo;
    
    private AuthResult(boolean success, String errorMessage, UserInfo userInfo) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.userInfo = userInfo;
    }
    
    public static AuthResult success(UserInfo userInfo) {
        return new AuthResult(true, null, userInfo);
    }
    
    public static AuthResult success() {
        return new AuthResult(true, null, null);
    }
    
    public static AuthResult failure(String errorMessage) {
        return new AuthResult(false, errorMessage, null);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public UserInfo getUserInfo() {
        return userInfo;
    }
}