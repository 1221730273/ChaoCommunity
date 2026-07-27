package com.ljc.chaocommunity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt 密码加密器
     *
     * BCrypt 的特点：
     * - 每次加密自动生成随机盐，同一密码两次加密结果不同
     * - encode()   → 加密密码
     * - matches()  → 比对明文和密文是否匹配
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤链
     * TODO 后续实现完整的JWT认证后再放开权限控制
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 无状态，不创建session（RESTful API + JWT 的标准做法）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 暂时放行所有请求（后续替换为JWT过滤器）
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // 关闭CSRF（前后端分离，不需要浏览器表单防护）
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
