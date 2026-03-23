package com.swpu.equipment.common.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component; // 必须加这个注解

import com.swpu.equipment.user.entity.User;
import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

@Component // 核心：让Spring扫描并创建Bean
public class TokenUtil {
    private static final Logger log = LoggerFactory.getLogger(TokenUtil.class);

    @Value("${jwt.secret:swpu-equipment-2026}")
    private String secret;

    @Value("${jwt.expire:7200000}")
    private long expire;

    @Autowired
    private com.swpu.equipment.user.service.UserService userService;

    // 生成Token
    public String generateToken(Long userId) {
        try {
            log.info("开始生成Token：userId={}", userId); // 强制打印
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            
            // 查询用户角色
            User user = userService.getById(userId);
            String role = user != null ? user.getRole().getValue() : "student";
            
            String token = Jwts.builder()
                    .setSubject(userId.toString())
                    .claim("role", role) // 添加角色信息
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expire))
                    .signWith(key)
                    .compact();
            log.info("Token生成完成：userId={}, role={}, token={}", userId, role, token);
            return token;
        } catch (Exception e) {
            log.error("Token生成失败：userId={}", userId, e);
            throw new RuntimeException("生成Token失败", e);
        }
    }

    // 解析Token
    public Long getUserIdFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            String userIdStr = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token.replace("Bearer ", ""))
                    .getBody()
                    .getSubject();
            return Long.parseLong(userIdStr);
        } catch (Exception e) {
            log.error("解析Token失败：token={}", token, e);
            return null;
        }
    }

    // 获取用户角色
    public String getRoleFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            String role = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token.replace("Bearer ", ""))
                    .getBody()
                    .get("role", String.class);
            return role;
        } catch (Exception e) {
            log.error("解析Token角色失败：token={}", token, e);
            return null;
        }
    }

    // 判断是否是管理员
    public boolean isAdministrator(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            String userIdStr = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token.replace("Bearer ", ""))
                    .getBody()
                    .getSubject();
            Long userId = Long.parseLong(userIdStr);
            
            // 查询数据库获取用户角色
            User user = userService.getById(userId);
            return user != null && user.isAdmin();
        } catch (Exception e) {
            log.error("解析Token失败：token={}", token, e);
            return false;
        }
    }

    // 创建Authentication对象，包含角色权限
    public UsernamePasswordAuthenticationToken createAuthentication(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            String role = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token.replace("Bearer ", ""))
                    .getBody()
                    .get("role", String.class);
            
            if (role != null) {
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                return new UsernamePasswordAuthenticationToken(token, null, Collections.singletonList(authority));
            }
            return null;
        } catch (Exception e) {
            log.error("创建Authentication失败：token={}", token, e);
            return null;
        }
    }
}