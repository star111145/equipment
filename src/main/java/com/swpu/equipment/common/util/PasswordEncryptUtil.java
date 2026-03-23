package com.swpu.equipment.common.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 */
@Component
public class PasswordEncryptUtil {

    private final PasswordEncoder passwordEncoder;

    // 注入Spring容器中的PasswordEncoder（已在SecurityConfig中配置）
    public PasswordEncryptUtil(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 加密密码（不可逆）
     * @param rawPassword 明文密码
     * @return 加密后的密码
     */
    public String encrypt(String rawPassword) {
        return passwordEncoder.encode(rawPassword);//返回字符串：$2a$10$xxxx
    }

    /**
     * 验证密码是否匹配
     * @param rawPassword 明文密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
