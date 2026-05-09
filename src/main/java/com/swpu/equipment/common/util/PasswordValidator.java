package com.swpu.equipment.common.util;

import com.swpu.equipment.system.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {
    
    @Autowired
    private SystemService systemService;
    
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
    
    public String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }
        
        int minLength = 8;
        boolean requireSpecialChar = false;
        
        try {
            String minLengthStr = systemService.getConfigValue("security.password_min_length");
            String requireSpecialStr = systemService.getConfigValue("security.password_require_special_char");
            
            if (minLengthStr != null) {
                minLength = Integer.parseInt(minLengthStr);
            }
            if (requireSpecialStr != null) {
                requireSpecialChar = Boolean.parseBoolean(requireSpecialStr);
            }
        } catch (Exception e) {
        }
        
        if (password.length() < minLength) {
            return "密码长度不能少于" + minLength + "位";
        }
        
        if (requireSpecialChar) {
            boolean hasSpecialChar = false;
            for (char c : SPECIAL_CHARS.toCharArray()) {
                if (password.indexOf(c) != -1) {
                    hasSpecialChar = true;
                    break;
                }
            }
            if (!hasSpecialChar) {
                return "密码必须包含特殊字符";
            }
        }
        
        return null;
    }
}
