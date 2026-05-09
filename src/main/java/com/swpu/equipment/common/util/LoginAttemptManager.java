package com.swpu.equipment.common.util;

import com.swpu.equipment.system.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginAttemptManager {
    
    @Autowired
    private SystemService systemService;
    
    private static final java.util.Map<String, LoginAttempt> attempts = new java.util.concurrent.ConcurrentHashMap<>();
    
    private static class LoginAttempt {
        int failCount;
        long lockUntil;
        
        LoginAttempt(int failCount, long lockUntil) {
            this.failCount = failCount;
            this.lockUntil = lockUntil;
        }
    }
    
    public String checkLoginAttempt(String identifier) {
        int maxAttempts = 5;
        int lockoutMinutes = 30;
        
        try {
            String maxAttemptsStr = systemService.getConfigValue("security.login_max_attempts");
            String lockoutStr = systemService.getConfigValue("security.login_lockout_minutes");
            
            if (maxAttemptsStr != null) {
                maxAttempts = Integer.parseInt(maxAttemptsStr);
            }
            if (lockoutStr != null) {
                lockoutMinutes = Integer.parseInt(lockoutStr);
            }
        } catch (Exception e) {
        }
        
        LoginAttempt attempt = attempts.get(identifier);
        long now = System.currentTimeMillis();
        
        if (attempt != null) {
            if (attempt.lockUntil > now) {
                long remainingMinutes = (attempt.lockUntil - now) / 60000 + 1;
                return "账号已被锁定，请" + remainingMinutes + "分钟后再试";
            }
            
            if (attempt.failCount >= maxAttempts) {
                attempt.failCount = 0;
                attempt.lockUntil = now + (lockoutMinutes * 60000L);
                attempts.put(identifier, attempt);
                return "登录失败次数过多，账号已锁定" + lockoutMinutes + "分钟";
            }
        }
        
        return null;
    }
    
    public void recordSuccess(String identifier) {
        attempts.remove(identifier);
    }
    
    public void recordFailure(String identifier) {
        int maxAttempts = 5;
        int lockoutMinutes = 30;
        
        try {
            String maxAttemptsStr = systemService.getConfigValue("security.login_max_attempts");
            String lockoutStr = systemService.getConfigValue("security.login_lockout_minutes");
            
            if (maxAttemptsStr != null) {
                maxAttempts = Integer.parseInt(maxAttemptsStr);
            }
            if (lockoutStr != null) {
                lockoutMinutes = Integer.parseInt(lockoutStr);
            }
        } catch (Exception e) {
        }
        
        LoginAttempt attempt = attempts.get(identifier);
        long now = System.currentTimeMillis();
        
        if (attempt == null) {
            attempt = new LoginAttempt(0, 0);
        }
        
        attempt.failCount++;
        
        if (attempt.failCount >= maxAttempts) {
            attempt.lockUntil = now + (lockoutMinutes * 60000L);
        }
        
        attempts.put(identifier, attempt);
    }
}
