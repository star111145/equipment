package com.swpu.equipment.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

//跨域配置
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Spring Boot 3 推荐用originPatterns，兼容所有前端域名
       // config.addAllowedOriginPattern("*");
        config.addAllowedOrigin("http://localhost:8090");
        // 允许所有请求方法（GET/POST/PUT/DELETE）
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许携带Cookie（前端请求需配withCredentials: true）
        config.setAllowCredentials(true);
        // 预检请求缓存时间（减少OPTIONS请求）
        config.setMaxAge(3600L);

        // 匹配所有接口（包括/api前缀）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}