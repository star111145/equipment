package com.swpu.equipment.common.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component; // 必须加这个注解

import javax.crypto.SecretKey;
import java.util.Date;

@Component // 核心：让Spring扫描并创建Bean
public class TokenUtil {
    private static final Logger log = LoggerFactory.getLogger(TokenUtil.class);

    @Value("${jwt.secret:swpu-equipment-2026}")
    private String secret;

    @Value("${jwt.expire:7200000}")
    private long expire;

    // 生成Token
    public String generateToken(Long userId) {
        try {
            log.info("开始生成Token：userId={}", userId); // 强制打印
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            String token = Jwts.builder()
                    .setSubject(userId.toString())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expire))
                    .signWith(key)
                    .compact();
            log.info("Token生成完成：userId={}, token={}", userId, token);
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
}