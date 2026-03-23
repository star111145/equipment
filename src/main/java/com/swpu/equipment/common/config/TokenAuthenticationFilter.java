package com.swpu.equipment.common.config;

import com.swpu.equipment.common.util.TokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
/**
 * 自定义Token认证过滤器
 * 用于从请求头中提取Token并进行认证
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthenticationFilter.class);

    @Autowired
    private TokenUtil tokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String authorization = request.getHeader("Authorization");
        log.debug("请求路径: {}, 请求头 Authorization: {}", requestURI, authorization);
        
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.replace("Bearer ", "");
            log.debug("提取的 Token: {}", token);
            
            // 验证Token并获取用户角色
            String role = tokenUtil.getRoleFromToken(token);
            log.debug("从 Token 中获取的角色: {}", role);
            
            if (role != null) {
                // 创建认证对象，包含角色权限
                Authentication authentication = tokenUtil.createAuthentication(token);
                log.debug("创建的 Authentication: {}", authentication);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("认证信息已设置到 SecurityContextHolder");
                }
            }
        } else {
            log.debug("请求路径: {} 没有找到 Authorization 请求头或不是 Bearer Token", requestURI);
        }
        
        filterChain.doFilter(request, response);
    }
}
