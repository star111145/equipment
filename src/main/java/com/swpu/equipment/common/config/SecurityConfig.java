package com.swpu.equipment.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

//Spring Security基础配置
/**
 * Spring Security配置（适配Spring Boot 3.5.11，无弃用方法）
 */
@Configuration
@EnableWebSecurity // 启用Web安全（3.x无需@EnableGlobalMethodSecurity）
@EnableMethodSecurity(prePostEnabled = true) // 启用方法级安全（替代@EnableGlobalMethodSecurity）
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
     * 替换旧语法（已弃用）为新语法（Spring Security 6.1+）
     * 
     * http
     *     .cors().and()
     *     .csrf().disable()
     *     .authorizeHttpRequests(auth -> auth
     *         .requestMatchers("/api/**").permitAll()
     *         .anyRequest().permitAll()
     *     )
     *     .formLogin()
     *     .disable()
     *     .logout()
     *     .logoutUrl("/api/logout")
     *     .permitAll();
     * 
     */
    //新语法（Spring Security 6.1+）
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            TokenAuthenticationFilter tokenAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                // 启用跨域配置（使用CorsConfig中的CorsConfigurationSource）
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // 开发阶段关闭CSRF（简化前端请求）
                .csrf(csrf -> csrf.disable())
                // 权限规则配置
                .authorizeHttpRequests(auth -> auth
                        // 放行静态资源、登录、注册等无需权限的接口
                        .requestMatchers("/login", "/logout", "/register").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/api/uploads/**", "/uploads/**").permitAll()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                // 禁用表单登录（使用Token认证）
                .formLogin(formLogin -> formLogin.disable())
                // 禁用HTTP Basic认证
                .httpBasic(httpBasic -> httpBasic.disable())
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .permitAll()
                )
                // 添加自定义认证过滤器
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }

    // /**
    //  * 内存用户配置（测试用，后续替换为数据库用户）
    //  * 密码通过passwordEncoder加密，匹配你yaml中的admin/123456
    //  */
    // @Bean
    // public UserDetailsService userDetailsService() {
    //     // 加密密码：123456 → 加密后存储（避免明文）
    //     String encodedPassword = passwordEncoder().encode("123456");

    //     // 创建admin用户，赋予ADMIN角色
    //     UserDetails admin = User.withUsername("admin")
    //             .password(encodedPassword) // 加密后的密码
    //             .roles("ADMIN") // 角色配置
    //             .build();

    //     // 内存用户管理器
    //     return new InMemoryUserDetailsManager(admin);
    // }
}