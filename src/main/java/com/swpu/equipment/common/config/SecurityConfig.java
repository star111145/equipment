package com.swpu.equipment.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

//Spring Security基础配置
/**
 * Spring Security配置（适配Spring Boot 3.5.11，无弃用方法）
 */
@Configuration
@EnableWebSecurity // 启用Web安全（3.x无需@EnableGlobalMethodSecurity）
public class SecurityConfig {

    /**
     * 密码编码器（核心：BCryptPasswordEncoder是3.x唯一推荐的加密方式）
     * 替代所有弃用的密码加密方法
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt算法：不可逆加密，自带盐值，无需手动处理
        return new BCryptPasswordEncoder();//加密强度采用默认值10
    }

    /**
     * 安全过滤链（替代旧的WebSecurityConfigurerAdapter）
     * 配置接口权限、跨域、CSRF等
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 启用跨域配置（关联CorsConfig）
                .cors().and()
                // 开发阶段关闭CSRF（简化前端请求）
                .csrf().disable()
                // 权限规则配置
                .authorizeHttpRequests(auth -> auth
                        // 放行所有/api开头的接口（开发阶段，后续可精细化）
                        .requestMatchers("/api/**").permitAll()
//                        // 其他所有请求需要认证（不适用）
//                        .anyRequest().authenticated()
                        // 兜底规则：其他请求也放行（避免任何重定向）
                        .anyRequest().permitAll()
                )
                // 保留登录页（可选，后续测试登录功能）
                .formLogin()
                .loginPage("/api/login") // 自定义登录页路径（避免和业务接口冲突，同时与接口前缀保持一致）
                .permitAll() // 放行登录页
                .and()
                .logout().permitAll(); // 放行登出接口

        return http.build();
    }

    /**
     * 内存用户配置（测试用，后续替换为数据库用户）
     * 密码通过passwordEncoder加密，匹配你yaml中的admin/123456
     */
    @Bean
    public UserDetailsService userDetailsService() {
        // 加密密码：123456 → 加密后存储（避免明文）
        String encodedPassword = passwordEncoder().encode("123456");

        // 创建admin用户，赋予ADMIN角色
        UserDetails admin = User.withUsername("admin")
                .password(encodedPassword) // 加密后的密码
                .roles("ADMIN") // 角色配置
                .build();

        // 内存用户管理器
        return new InMemoryUserDetailsManager(admin);
    }
}